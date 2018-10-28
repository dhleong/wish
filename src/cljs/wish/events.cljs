(ns wish.events
  (:require-macros [wish.util.log :refer [log]])
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx
                                   path
                                   inject-cofx trim-v]]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [vimsical.re-frame.cofx.inject :as inject]
            [wish.db :as db]
            [wish.fx :as fx]
            [wish.inventory :as inv]
            [wish.push :as push]
            [wish.providers :as providers]
            [wish.sheets.util :refer [update-uses update-sheet update-sheet-path]]
            [wish.subs-util :refer [active-sheet-id]]
            [wish.util :refer [invoke-callable]]))

(reg-event-fx
  ::initialize-db
  (fn-traced [_ _]
    {:db db/default-db
     :fetch-latest-update :!
     :providers/init! :!}))

(reg-event-fx
  :navigate!
  [trim-v]
  (fn-traced [{:keys [db]} page-spec]
    {:db (assoc db :page page-spec)
     :dispatch-n [[::update-keymap page-spec]
                  [:push/check]]}))

(reg-event-db
  :set-device
  [trim-v]
  (fn-traced [db [device-type]]
    (assoc db :device-type device-type)))

(reg-event-fx
  ::update-keymap
  [trim-v (inject-cofx ::inject/sub [:meta/kind])]
  (fn-traced [{sheet-kind :meta/kind} [page-spec]]
    {::fx/update-keymaps [page-spec
                          sheet-kind]}))

; expects a full reagent form, eg: [#'hp-overlay]
(reg-event-db
  :toggle-overlay
  [trim-v]
  (fn-traced [db [[overlay-fn & args :as overlay-spec] & {:keys [scrollable?] :as opts}]]
    (if overlay-spec
      (update db
              :showing-overlay
              (fn [old new-spec]
                (when-not old
                  new-spec))
              [(if scrollable?
                 "overlay-scrollable"
                 "overlay")
               overlay-spec])

      ; always dismiss
      (assoc db :showing-overlay nil))))

(reg-event-fx
  :title!
  [trim-v]
  (fn-traced [_ [title]]
    {:title! title}))

(reg-event-fx
  :set-online
  [trim-v]
  (fn [{:keys [db]} [online?]]
    ; NOTE: we can't use fn-traced here due to cond-> use
    ; See: https://github.com/Day8/re-frame-debux/issues/22

    (log "online <- " online?)
    (cond-> {:db (assoc db :online? online?)}

      ; if we're coming back online, trigger init!
      online? (assoc :providers/init! :!))))

(reg-event-fx
  :set-latest-update
  [trim-v]
  (fn [{:keys [db]} [version]]
    {:persist-latest-update version
     :db (assoc-in db [:updates :latest] version)}))

(reg-event-fx
  :update-app
  [trim-v]
  (fn []
    {:dispatch [:ignore-latest-update]
     :update-app :now!}))

(reg-event-db
  :ignore-latest-update
  [trim-v]
  (fn [db _]
    (update db :updates (fn [updates]
                          (assoc updates :ignored (:latest updates))))))

(reg-event-fx
  :set-ignored-update
  [trim-v]
  (fn [{:keys [db]} [ignored-version]]
    {:db (assoc-in db [:updates :ignored] ignored-version)
     :notify-service-worker (when (:worker-ready? db)
                              :ready)}))

(reg-event-fx
  :set-worker-ready
  [trim-v]
  (fn [{:keys [db]} _]
    {:db (assoc db :worker-ready? true)
     :notify-service-worker (when-not (= :unknown (get-in db [:updates :ignored]))
                              :ready)}))



; ======= Provider management ==============================

(reg-event-db
  :prepare-provider-states!
  [trim-v (path :provider-states)]
  (fn-traced [states [provider-ids]]
    (reduce
      (fn [old-states provider-id]
        (if-not (contains? old-states provider-id)
          ; only set the "pending" state (nil) if
          ; there was no existing state
          (assoc old-states provider-id nil)

          ; old state already; do nothing
          old-states))
      states
      provider-ids)))

(reg-event-fx
  :put-provider-state!
  [trim-v]
  (fn-traced [{:keys [db]} [provider-id state]]
    (let [can-query? (contains? #{:ready :cache-only} state)
          will-query? (when can-query?
                        ; only query again if we previously could not
                        (not= :ready
                              (get-in db [:provider-states provider-id])))]
      {:db (cond-> db
             ; always put the state
             true (assoc-in [:provider-states provider-id] state)

             ; if it's ready to query, immediately mark it as listing
             will-query? (update :providers-listing conj provider-id))

       :providers/query-sheets (when will-query?
                                 provider-id)})))

(reg-event-db
  :mark-provider-listing!
  [trim-v]
  (fn-traced [db [provider-id listing?]]
    (let [method (if listing?
                   conj
                   disj)]
      (update db :providers-listing method provider-id))))

(reg-event-fx
  :query-data-sources
  [trim-v]
  (fn-traced [cofx]
    {:providers/query-data-sources :query}))


; ======= data source management ===========================

; expects infos to be [{:id,:name}]
(reg-event-db
  :add-data-sources
  [trim-v]
  (fn-traced [db [infos]]
    (reduce
      (fn [db info]
        (assoc-in db
                  [:data-sources (:id info)]
                  info))
      db
      infos)))


; ======= sheet-meta + builder stuff =======================

; this is probably too general...
(reg-event-fx
  :update-meta
  [trim-v]
  (fn-traced [cofx [path f & args]]
    (apply update-sheet-path cofx path f args)))

(reg-event-fx
  :set-sheet-data-sources
  [trim-v]
  (fn-traced [cofx [sheet-id data-sources]]
    (assoc-in
      (update-sheet-path cofx
                         [:sources]
                         (constantly (vec data-sources)))

      ; delete the sheet source to trigger a reload
      [:db :sheet-sources sheet-id] nil)))

; ======= sheet-related ====================================

(defn- reset-sheet-err
  [db sheet-id]
  (if (get-in db [:sheet-sources sheet-id :err])
    ; we previous error'd; since we've loaded the sheet now,
    ; let's reset the entire source so the UI doesn't think
    ; it's still error'd while we reload it
    (update db :sheet-sources dissoc sheet-id)

    ; normal case
    db))

(reg-event-fx
  :load-sheet!
  [trim-v]
  (fn-traced [{:keys [db]} [sheet-id]]
    {:load-sheet! sheet-id
     :db (reset-sheet-err db sheet-id)}))

; sheet loaded
(reg-event-fx
  :put-sheet!
  [trim-v]
  (fn-traced [{:keys [db]} [sheet-id sheet]]
    {:db (-> db
             (assoc-in [:sheets sheet-id] sheet)
             (reset-sheet-err sheet-id))

     ; NOTE: we're probably on a :sheet page, and we might not have
     ; known the sheet-kind when we first navigated so we should
     ; try to update-keymap again now that we (should) know it
     :dispatch [::update-keymap (:page db)]}))

(reg-event-db
  :put-sheet-error!
  [trim-v]
  (fn-traced [db [sheet-id info]]
    (assoc-in db [:sheet-sources sheet-id] info)))

(reg-event-fx
  :retry-current-sheet!
  [trim-v
   (inject-cofx ::inject/sub [:sheet-error-info])]
  (fn-traced [{:keys [sheet-error-info]} _]
    (when-let [retry-evt (:retry-evt sheet-error-info)]
      (log "Retrying sheet: " retry-evt)
      {:dispatch retry-evt})))

(reg-event-db
  :add-sheets
  [trim-v]
  (fn-traced [db [sheet-id-pairs]]
    (-> db
        ; NOTE: since the value in :sheets will be overwritten
        ; later, we copy special values like :mine? into their
        ; own things. See the discussion in the :known-sheets sub.
        (update :my-sheets
                #(apply conj %
                        (keep
                          (fn [[sheet-id data]]
                            (when (:mine? data)
                              sheet-id))
                          sheet-id-pairs)))

        (update :sheets
                (fn [sheets sheet-id-pairs]
                  (reduce
                    (fn [m [id data]]
                      (if-not (get m id)
                        (assoc m id data)

                        ; we've already loaded this sheet; don't delete it
                        m))
                    sheets
                    sheet-id-pairs))
                sheet-id-pairs))))

(reg-event-fx
  :load-sheet-source!
  [trim-v]
  (fn-traced [{:keys [db]} [sheet sources]]
    ; no dup loads, pls
    (let [sheet-id (:id sheet)
          source (get-in db [:sheet-sources sheet-id])]
      (when (or (:err source)
                (not source))
        {:db (assoc-in db [:sheet-sources sheet-id] {})
         :load-sheet-source! [sheet sources]}))))

(reg-event-db
  :put-sheet-source!
  [trim-v]
  (fn-traced [db [sheet-id source]]
    (assoc-in db [:sheet-sources sheet-id]
              {:loaded? true
               :source source})))

; Internal event triggered by the :schedule-save fx
(reg-event-fx
  ::fx/save-sheet!
  [trim-v]
  (fn-traced [{:keys [db]} [sheet-id]]
    ; fetch the sheet data and forward it to the ::save-sheet! fx handler
    {::fx/save-sheet! [sheet-id (get-in db [:sheets sheet-id])]}))

; used by the CachingProvider to eagerly persist offline changes to a sheet
(reg-event-fx
  :persist-cached-sheet!
  [trim-v]
  (fn-traced [{:keys [db]} [sheet-id data-str]]
    {::fx/save-sheet! [sheet-id data-str :from-cache]}))

(reg-event-fx
  :share-sheet!
  [trim-v]
  (fn-traced [_ [sheet-id]]
    {:share-sheet! sheet-id}))


; ======= Sheet browsing ==================================

(reg-event-db
  :filter-sheets
  [trim-v (path :sheets-filters)]
  (fn-traced [filters [filter-key active?]]
    (let [{:keys [mine? shared?]
           :as new-filters}
          (assoc filters filter-key active?)]
      (if-not (or mine? shared?)
        ; if neither is checked, auto-check the "other"
        ; one after unchecking the last
        (case filter-key
          :mine? (assoc new-filters :shared? true)
          :shared? (assoc new-filters :mine? true))

        ; leave as-is
        new-filters))))


; ======= Limited-use handling =============================

(defn restore-trigger-matches?
  [required actual]
  (cond
    (keyword? required) (= required actual)
    (set? required) (contains? required actual)
    (coll? required) (contains? (set required)
                                actual)))

(defn apply-limited-use-trigger
  [limited-used-map limited-uses trigger]
  (reduce-kv
    (fn [m use-id used]
      (if-let [use-obj (get limited-uses use-id)]
        (if (restore-trigger-matches?
              (:restore-trigger use-obj)
               trigger)
          (let [restore-amount (invoke-callable
                                 use-obj
                                 :restore-amount
                                 :used used
                                 :trigger trigger)
                new-amount (max 0
                                (- used
                                   restore-amount))]
            (assoc m use-id new-amount))

          ; wrong trigger; ignore
          m)

        ; else (no use-obj):
        (do
          ; NOTE: we *could* just dissoc the use-id, but it's only a few
          ; bytes, and it's possible that the user has accidentally gotten
          ; into a state where the use-obj is missing, and will later
          ; get out of that state (IE: restore a dropped class). Let's
          ; avoid accidentally munging potentially-useful data.
          (js/console.warn "Found unrelated limited-use " use-id " !!")
          m)))
    limited-used-map
    limited-used-map))

(reg-event-fx
  :trigger-limited-use-restore
  [trim-v
   (inject-cofx ::inject/sub ^:ignore-dispose [:limited-uses-map])
   (inject-cofx ::inject/sub [:active-sheet-id])]
  (fn-traced [{:keys [db limited-uses-map active-sheet-id]} [triggers]]
    {:db (reduce
           (fn [db trigger]
             (update-in db [:sheets active-sheet-id :limited-uses]
                        apply-limited-use-trigger
                        limited-uses-map
                        trigger))
           db
           (if (coll? triggers)
             triggers
             [triggers]))}))

; toggle whether a single-use limited-use item has been used
(reg-event-fx
  :toggle-used
  [trim-v]
  (fn-traced [cofx [use-id]]
    (update-uses cofx use-id (fn [uses]
                               (if (> uses 0)
                                 0
                                 1)))))

; add some number of uses
(reg-event-fx
  :+use
  [trim-v]
  (fn-traced [cofx [use-id amount]]
    (update-uses cofx use-id + amount)))

; accepts a map of use-id -> amount
(reg-event-fx
  :+uses
  [trim-v]
  (fn-traced [cofx [use-id->amount]]
    (reduce-kv
      (fn [new-cofx use-id amount]
        (update-uses new-cofx use-id + amount))
      cofx
      use-id->amount)))


; ======= option-handling ==================================

; Update the presence of an entry in the given option set
; method must be either conj or disj
(reg-event-fx
  :update-option-set
  [trim-v]
  (fn [cofx [option-id method entry]]
    (update-sheet-path cofx [:options option-id]
                       (fn [old-v m e]
                         (m
                           (or old-v #{})
                           e))
                       method
                       entry)))


; ======= inventory management =============================

(defn inventory-add
  "Add an item to a sheet's :inventory, creating an :items entry
   if necessary (EG: for non-stacked or custom items). A custom
   item is one without an :id (one will be generated)."
  ([sheet item]
   (inventory-add sheet item nil))
  ([sheet item quantity]
   (let [quantity (or quantity
                      (-> item :default-quantity)
                      1)
         item-id (:id item)
         custom? (inv/custom? item)

         item-id (or (:item-id item)

                     ; generate a new item-id
                     (when custom?
                       (inv/custom-id (:name item)))

                     ; had an id already
                     item-id)

         inst-id (cond
                   ; custom items use their generated id
                   custom? item-id

                   ; generate an instance id
                   (not (inv/stacks? item))
                   (inv/instantiate-id item-id))

         sheet (if inst-id
                 ; we instantiated
                 (assoc-in sheet [:items inst-id]
                           (if custom?
                             (assoc item :id item-id)
                             {:id item-id}))

                 ; nothing to do
                 sheet)
         inst-id (or inst-id
                     (:id item))]
     (update-in sheet [:inventory inst-id] + quantity))))

(defn- do-add
  [cofx item quantity]
  (log "add" item "<-" quantity)

  ; update the sheet meta directly
  (if (or (not quantity)
          (inv/stacks? item))
    (update-sheet-path cofx [] inventory-add item quantity)

    ; instantiate each one
    (reduce
      #(update-sheet-path %1 [] inventory-add item)
      cofx
      (range quantity))))

(reg-event-fx
  :inventory-add
  [trim-v]
  (fn [cofx [item & [quantity]]]
    (do-add cofx item quantity)))

; add many items at once; an "item" can be an item map directly
; or a pair of [item amount] to add multiple
(reg-event-fx
  :inventory-add-n
  [trim-v]
  (fn [cofx [items]]
    (reduce
      (fn [cofx item]
        (if (map? item)
          ; single item
          (do-add cofx item nil)

          ; [item, amount] pair
          (do-add cofx (first item) (second item))))
      cofx
      items)))


; NOTE: unlike :inventory-add and :inventory-subtract, this
; is ONLY intended to change the current quantity of a stacks?
; item and (at least for now) does not attempt to instantiate
; items or anything fancy like that.
; It will, however, enforce a min value of 0
(reg-event-fx
  :inventory-set-amount
  [trim-v]
  (fn [cofx [item quantity]]
    (update-sheet-path cofx [:inventory]
                       assoc
                       (:id item)
                       (max 0 quantity))))

(defn inventory-subtract
  ([sheet item]
   (inventory-subtract sheet item nil))
  ([sheet item quantity]
   (let [quantity (or quantity 1)
         inst-id (:id item)

         ; base, initial update
         sheet (update-in sheet [:inventory inst-id] - quantity)
         new-amount (get-in sheet [:inventory inst-id])]

     (cond
       ; easy case; still > 0
       (> new-amount 0) sheet

       ; if the item is *not* a stacks? custom item, remove it from
       ; :items if its quantity became <= 0
       (or (not (inv/stacks? item))
           ; it's custom if :id in :items = inst-id
           (not= inst-id (get-in sheet [:items inst-id :id])))
       (-> sheet
           (update :items dissoc inst-id)
           (update :inventory dissoc inst-id))

       ; otherwise, it's a :stacks? custom item; just ensure the quantity
       ; is not less than 0
       :else (assoc-in sheet [:inventory inst-id] 0)))))

; subtract some amount (default 1) from the quantity of an item
; instance in inventory. If the quantity goes <= 0, the instance in
; :items will be removed—unless the item is a :stacks? custom item,
; in which case it will just go to 0 quantity, so you don't have to
; re-create a custom ammunition, for example, when you run out.
; :stacks? custom items can be deleted from inventory if desired
; using :inventory-delete.
(reg-event-fx
  :inventory-subtract
  [trim-v]
  (fn [cofx [item & [quantity]]]
    ; update the sheet meta directly
    (update-sheet-path cofx [] inventory-subtract item quantity)))


; delete an item in the inventory regardless of its current quantity
; and custom-ness. Always removes the instance from :items
(reg-event-fx
  :inventory-delete
  [trim-v]
  (fn [cofx [item]]
    (update-sheet-path cofx []
                       (fn [sheet inst-id]
                         (-> sheet
                             (update :items dissoc inst-id)
                             (update :inventory dissoc inst-id)))
                       (:id item))))

; "edit" an instanced item in the inventory, replacing by id. usually for
; updating a custom item
(reg-event-fx
  :inventory-edit
  [trim-v]
  (fn [cofx [item]]
    (update-sheet-path cofx [:items]
                       assoc
                       (:id item)
                       (-> item
                           (assoc :id (:item-id item))
                           (dissoc :item-id)))))

(reg-event-fx
  :toggle-equipped
  [trim-v]
  (fn [cofx [item]]
    (update-sheet-path cofx [:equipped]
                       (fn [equipped inst-id]
                         (if (get equipped inst-id)
                           (disj equipped inst-id)
                           (conj equipped inst-id)))
                       (:id item))))


; ======= Save-state handling ==============================

(reg-event-db
  ::db/put-pending-save
  [trim-v]
  (fn [db [sheet-id]]
    (update db ::db/pending-saves conj sheet-id)))

(reg-event-db
  ::db/mark-save-processing
  [trim-v]
  (fn [db [sheet-id]]
    (-> db
        (update ::db/pending-saves disj sheet-id)
        (update ::db/save-errors disj sheet-id)
        (update ::db/processing-saves conj sheet-id))))

(reg-event-db
  ::db/finish-save
  [trim-v]
  (fn [db [sheet-id err]]
    (-> db
        (update ::db/pending-saves disj sheet-id)
        (update ::db/processing-saves disj sheet-id)
        (update ::db/save-errors (if err
                                   conj
                                   disj) sheet-id))))


; ======= Push notifications ==============================

(reg-event-fx
  :push/check
  [(inject-cofx ::inject/sub ^:ignore-dispose [:interested-push-ids])]
  (fn [{ids :interested-push-ids}]
    (if (seq ids)
      {:push/ensure ids}
      {:push/disconnect :!})))

(reg-event-fx
  ::push/session-created
  [trim-v (inject-cofx ::inject/sub ^:ignore-dispose [:interested-push-ids])]
  (fn [{current-ids :interested-push-ids} [interested-ids session-id]]
    (if-not (= current-ids interested-ids)
      (log "Drop un-interesting sesssion; was " interested-ids "; now " current-ids)
      {:push/connect session-id})))
