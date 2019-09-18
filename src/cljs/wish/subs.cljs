(ns wish.subs
  (:require-macros [wish.util.log :as log])
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub subscribe]]
            [wish-engine.core :as engine]
            [wish.data :as data]
            [wish.db :as db]
            [wish.inventory :as inv]
            [wish.providers :as providers]
            [wish.subs-util :refer [active-sheet-id reg-id-sub]]
            [wish.util :refer [deep-merge padded-compare]]))

(reg-sub :device-type :device-type)
(reg-sub :showing-overlay :showing-overlay)

(reg-sub
  :notifications
  (fn [db]
    (->> db
         :notifications
         vals
         (sort-by :created))))

(reg-sub
  :update-available?
  (fn [{{:keys [latest ignored]} :updates} _]
    (and (not (nil? ignored)) ; if nil, this is a fresh run
         (not (nil? latest))
         (not= :unknown ignored)
         (not= latest ignored))))


; ======= Provider-related =================================

(reg-sub :provider-states :provider-states)
(reg-sub
  :provider-state
  :<- [:provider-states]
  (fn [states [_ provider-id]]
    (when-not provider-id
      (log/warn "nil provider-id provided to :provider-state"))
    (get states provider-id)))

(reg-sub
  :storable-provider-states
  :<- [:provider-states]
  (fn [states _]
    (->> states
         (remove (fn [[k _]]
                   (= :wish k))))))

(reg-sub
  :any-storable-provider?
  :<- [:storable-provider-states]
  (fn [states _]
    (->> states
         (map second)
         (into #{})
         :ready)))

(reg-sub
  :providers-listing?
  (fn [db _]
    (or (seq (:providers-listing db))

        ; if provider-states is empty, then providers/init!
        ; hasn't yet been called/finished executing
        (empty? (:provider-states db))

        (some (fn [[_id state]]
                (nil? state))
              (:provider-states db)))))


; ======= data sources =====================================

(reg-sub
  :data-sources
  (fn [db _]
    (->> db
         :data-sources
         vals
         seq)))


; ======= Sheet-related ====================================

(defn- reg-meta-sub
  [id getter]
  ; NOTE: instead of depending on a single subscription,
  ; we go ahead and create a separate subscription for
  ; each part of the sheet-meta, to avoid a small edit to HP,
  ; for example, causing all of the spell lists and features
  ; (which rely on classes, etc.) to be re-calculated
  (reg-id-sub
    id
    :<- [:sheet-meta]
    (fn [sheet _]
      (getter sheet))))

(reg-sub :page :page)
(reg-sub :sheets :sheets)
(reg-sub :my-sheets :my-sheets)
(reg-sub :sheets-filters :sheets-filters)
(reg-sub :sheet-sources :sheet-sources)

; sheets
(reg-meta-sub :meta/name :name)
(reg-meta-sub :meta/sheet :sheet)
(reg-meta-sub :meta/sources :sources)
(reg-meta-sub :meta/kind :kind)
(reg-meta-sub :meta/classes (comp vals :classes))
(reg-meta-sub :meta/races :races)
(reg-meta-sub :limited-used :limited-uses)
(reg-meta-sub :meta/options :options)
(reg-meta-sub :meta/inventory :inventory)
(reg-meta-sub :meta/items :items)
(reg-meta-sub :meta/effects :effects)
(reg-meta-sub :meta/equipped :equipped)
(reg-meta-sub :meta/campaign :campaign)

; campaigns
(reg-meta-sub :meta/players :players)

(reg-sub
  :active-sheet-source-ids
  :<- [:meta/sources]
  set)

(reg-sub
  :active-sheet-id
  :<- [:page]
  (fn [page-vec [_ ?requested-id]]
    ; NOTE: subscriptions created with wish.sub-util/reg-id-sub
    ; can accept an extra param in their query vector that will
    ; get passed down to us as ?requested-id, if provided; if
    ; not, we just do the normal thing and extract the
    ; active-sheet-id from the page vector
    (or ?requested-id
        (active-sheet-id nil page-vec))))

(reg-sub
  :sharable-sheet-id
  :<- [:active-sheet-id]
  :<- [:my-sheets]
  (fn [[sheet-id my-sheets] _]
    ; first, only if it's ours
    (when (contains? my-sheets sheet-id)
      ; then, the provider must be able to share it
      (when (providers/sharable? sheet-id)
        ; good to go
        sheet-id))))

(reg-sub
  :provided-sheet
  :<- [:sheets]
  (fn [sheets [_ sheet-id]]
    (assoc (get sheets sheet-id)
           :id sheet-id)))

(reg-sub
  ::known-files
  :<- [:sheets]
  :<- [:my-sheets]
  (fn [[sheets my-sheets] _]
    ; NOTE: on initial insert with add-sheets,
    ; :mine? is populated directly on the sheet, and
    ; we use that to populate :my-sheets. However,
    ; subsequent writes to :sheets might overwrite that,
    ; so we always use :my-sheets as the source of truth.
    ; Sure, we could copy over the value, but it's simpler
    ; if the value of a sheet's entry in :sheets when loaded
    ; is exactly the value in the provider.
    (->> sheets
         (map (fn [[id v]]
                (-> v
                    (assoc :id id
                           :mine? (contains? my-sheets id))

                    ; strip any file extensions from the name
                    (update :name str/replace data/sheet-extension-regex, ""))))
         (filter :name)
         (sort-by :name))))

(reg-sub
  :known-sheets
  :<- [::known-files]
  (fn [all-files _]
    (->> all-files
         (filter (comp (partial = :sheet)
                       :type))
         (sort-by :name))))

(reg-sub
  :filtered-known-sheets
  :<- [:known-sheets]
  :<- [:sheets-filters]
  (fn [[sheets filters] _]
    (cond
      (and (:mine? filters)
           (:shared? filters))
      sheets

      (:mine? filters)
      (filter :mine? sheets)

      (:shared? filters)
      (remove :mine? sheets))))

(reg-sub
  :known-campaigns
  :<- [::known-files]
  (fn [all-files _]
    (->> all-files
         (filter (comp (partial = :campaign)
                       :type))
         (sort-by :name))))


; if a specific sheet-id is not provided, loads
; for the active sheet id
(reg-id-sub
  :sheet-source
  :<- [:sheet-sources]
  :<- [:active-sheet-id]
  (fn [[sources active-id] [_ sheet-id]]
    (let [{:keys [source loaded?]} (get sources (or sheet-id
                                                    active-id))]
      (when loaded?
        source))))

(reg-id-sub
  :sheet-engine-state
  :<- [:sheet-source]
  (fn [source]
    (some->> source deref)))

(reg-sub
  :sheet-error-info
  :<- [:sheet-sources]
  :<- [:active-sheet-id]
  (fn [[sources active-id] [_ sheet-id]]
    (let [{:keys [err] :as info} (get sources (or sheet-id
                                                    active-id))]
      (when err
        info))))


; ======= Accessors for the active sheet ===================

(reg-id-sub
  :sheet-meta
  :<- [:sheets]
  :<- [:active-sheet-id]
  (fn [[sheets id]]
    (get sheets id)))

(reg-id-sub
  :classes
  :<- [:sheet-engine-state]
  :<- [:meta/options]
  :<- [:meta/classes]
  (fn [[state options metas] _]
    (when state
      (->> metas
           (map (fn [m]
                  (engine/inflate-class
                    state (:id m) m options)))))))

; A single class instance, or nil if none at all; if any
; class is marked primary, that class is returned. If none
; are so marked, then nil is returned
(reg-id-sub
  :primary-class
  :<- [:classes]
  (fn [classes]
    (->> classes
         (filter :primary?)
         first)))

; sum of levels from all classes
(reg-id-sub
  :total-level
  :<- [:classes]
  (fn [classes _]
    (apply + (map :level classes))))

(defn- inflate-effect [effect args]
  (cond
    (map? args)
    [(merge effect args)]

    (seq? args)
    (map (fn [arg] (inflate-effect effect arg)) args)

    :else
    [effect]))

(reg-id-sub
  :all-effects
  :<- [:sheet-engine-state]
  (fn [source _]
    (when source
      (vals (:effects source)))))

(reg-id-sub
  :all-effects/sorted
  :<- [:all-effects]
  (fn [effects _]
    (sort-by :name effects)))


(reg-id-sub
  :effects
  :<- [:sheet-engine-state]
  :<- [:meta/effects]
  (fn [[source effects] _]
    (when source
      (->> effects
           (mapcat (fn [[effect-id args]]
                     (when-let [effect (get-in source [:effects effect-id])]
                       (inflate-effect effect args))))
           (map (fn [effect]
                  ((:! effect) effect)))))))

(reg-id-sub
  :effect-ids-set
  :<- [:effects]
  (fn [effects _]
    (->> effects
         (map :id)
         (into #{}))))

(reg-id-sub
  :races
  :<- [:sheet-engine-state]
  :<- [:meta/options]
  :<- [:total-level]
  :<- [:meta/races]
  (fn [[state options total-level ids] _]
    (when state
      (->> ids
           (map (fn [id]
                  (engine/inflate-race
                    state
                    id
                    {:id id
                     :level total-level}
                    options)))))))

; combines :attrs from all classes and races into a single map
(reg-id-sub
  :all-attrs
  :<- [:classes]
  :<- [:races]
  :<- [:effects]
  (fn [entity-lists _]
    (->> entity-lists
         flatten
         (map :attrs)
         (apply merge-with deep-merge))))

(defn get-features
  "Returns a collection of [id feature] pairs."
  [feature-containers [_ entity-id]]
  (->> (if entity-id
         ; if we were provided an entity-id, only get features from that entity
         (filter #(= entity-id (:id %)) feature-containers)

         ; otherwise, combine them all
         feature-containers)

       ; combine features from all containers into a single collection,
       ; adding (meta) to each feature with the container it came from
       (mapcat (fn [container]
                 (map (fn [entry]
                        (with-meta
                          [(:id entry) entry]
                          {:wish/container-id (:id container)
                           :wish/container container}))
                      (:sorted-features container))))

       ; eagerly evaluate descriptions
       (map (fn [entry]
              (let [container (-> entry meta :wish/container)]
                (update-in entry [1 :desc]
                           (fn [desc]
                             (if (fn? desc)
                               (desc container)
                               desc))))))

       ; remove features that only the primary class should have
       ; if we're not the primary
       (remove (fn [[_id f :as entry]]
                 (when (:primary-only? f)
                   (let [primary? (->> entry
                                       meta
                                       :wish/container
                                       :primary?)]
                     (not primary?)))))))

(defn- filter-available
  "Updates all options, evaluating :available? 'in place'
   if provided, or setting to `true` if not. What we would
   prefer to do is actually (filter) the elements, but because
   reagent-forms doesn't handle a dynamically changing set of
   options for a :list, we set this flag so it can be later
   queried from a :visible? function"
  [available-map values]
  (map
    (fn [v]
      (assoc v :available?
             (if-let [available? (:available? v)]
               (available? available-map)

               ; if not provided, it's always available
               true)))
    values))

(defn- inflate-feature-options
  [[features source]]
  (->> features
       (map (fn [[id v :as entry]]
              (with-meta
                [id (-> v
                        (assoc :wish/raw-values (:values v))
                        (update :values (partial engine/inflate-entities source))

                        ; filter values, if a fn was provided
                        (as-> v
                          (if-let [filter-fn (:values-filter v)]
                            (update v :values (partial filter filter-fn))
                            v))
                        )]
                (meta entry))))
       (sort-by (comp :wish/sort second) padded-compare)))

(defn- only-feature-options
  [[features data-source]]
  (inflate-feature-options
    [(filter (comp :max-options second) features)
     data-source]))

(reg-id-sub
  :class-features
  :<- [:classes]
  get-features)

(reg-sub
  :inflated-class-features
  (fn [[_ entity-id]]
    [(subscribe [:class-features entity-id])
     (subscribe [:sheet-engine-state])])
  inflate-feature-options)

; like :inflated-class-features but removing features
; that don't accept options
(reg-sub
  :class-features-with-options
  (fn [[_ entity-id]]
    [(subscribe [:class-features entity-id])
     (subscribe [:sheet-engine-state])])
  only-feature-options)

(reg-id-sub
  :race-features
  :<- [:races]
  get-features)

(reg-id-sub
  :inflated-race-features
  :<- [:race-features]
  :<- [:sheet-engine-state]
  inflate-feature-options)

(reg-id-sub
  :race-features-with-options
  :<- [:race-features]
  :<- [:sheet-engine-state]
  only-feature-options)

; semantic convenience for single-race systems
(reg-id-sub
  :race
  :<- [:races]
  (fn [races _]
    (first races)))

(defn- uses-with-context
  [kind entity]
  (->> entity
       :limited-uses
       vals
       (map (fn [item]
              (assoc item
                     :wish/context-type kind
                     :wish/context entity)))))

;; NOTE: this used to be called `:limited-uses` as it gathered all the
;; available :limited-uses keys from all possible sources, but that's
;; easy to confuse with the `:limited-uses` key in a sheet, whose
;; subscription is called `:limited-used`.
(reg-id-sub
  :all-limited-use-configs
  :<- [:classes]
  :<- [:races]
  :<- [:inventory-map]
  (fn [[classes races inventory]]
    (flatten
      (concat
        (->> races
             (map (partial uses-with-context :race)))
        (->> classes
             (map (partial uses-with-context :class)))
        (->> inventory
             vals
             (map (partial uses-with-context :item)))))))

;; NOTE: this used to be `:limited-uses-map`, but for clarity
;; (and to better distinguish from the map called :limited-uses
;; in the character sheet) it's been renamed to more semantically
;; represent what it is
(reg-id-sub
  :limited-use-config
  :<- [:all-limited-use-configs]
  (fn [limited-use-configs]
    (reduce
      (fn [m v]
        (assoc m (:id v) v))
      {}
      limited-use-configs)))

(defn inflate-item
  "Given the character's :items map and datasource,
   return the inflated item for the given inst-id."
  [inst-id items data-source]
  (let [item (get items inst-id)
        item-id (if-not item
                  ; no items entry? must be an item-id already
                  inst-id

                  ; get the :id from the item entry, if any
                  (get item :id))
        item-state (merge
                     (get-in data-source [:items item-id])
                     (dissoc item :id) )]
    (engine/inflate-entity
            data-source
            item-state
            item-state
            {})))

; map of :inst-id -> inflated item in the active sheet's inventory,
; where each inflated item with an amount > 1 (or which :stacks?)
; includes the special key :wish/amount indicating that amount. The
; :id of each item will always be the instance id, and the :item-id
; will always be the (surprise) item-id.
; In addition, every item in :equipped will have the :wish/equipped?
; set to true
(reg-id-sub
  :inventory-map
  :<- [:meta/inventory]
  :<- [:meta/items]
  :<- [:meta/equipped]
  :<- [:sheet-engine-state]
  (fn [[raw-inventory items equipped data-source]]
    (reduce-kv
      (fn [m inst-id amount]
        (let [equipped? (get equipped inst-id)
              inflated (inflate-item inst-id items data-source)
              show-amount? (or (> amount 1)
                               (inv/stacks? inflated))
              item (if show-amount?
                     (assoc inflated :wish/amount amount)
                     inflated)
              item (if equipped?
                     (-> item
                         (assoc :wish/equipped? true)
                         #_(sheets/post-process
                           sheet-kind
                           data-source
                           :item))
                     item)]
          (assoc m inst-id
                 (assoc item
                        :id inst-id
                        :item-id (:id item)))))
      {}
      raw-inventory)))

; sorted list of inflated inventory items
(reg-id-sub
  :inventory-sorted
  :<- [:inventory-map]
  (fn [inventory-map]
    (->> inventory-map
         vals
         (sort-by :name))))

; sorted list of inflated + equipped inventory items
(reg-id-sub
  :equipped-sorted
  :<- [:inventory-sorted]
  (fn [inventory-sorted]
    (->> inventory-sorted
         (filter :wish/equipped?))))


; list of all known items for the current sheet
(reg-sub
  :all-items
  :<- [:sheet-engine-state]
  (fn [source]
    (->> source
         :items
         vals
         (sort-by :name))))


; ======= character builder-related ========================

(reg-sub
  :available-entities
  :<- [:sheet-engine-state]
  (fn [source [_ entity-kind]]
    (->> (get source entity-kind)
         vals)))

(reg-sub
  :options->
  :<- [:meta/options]
  (fn [options [_ path]]
    (let [v (get-in options path)
          {instanced-value :value} v]
      (if (and (:id v)
               instanced-value)
        instanced-value
        v))))


; ======= Save state =======================================

(reg-sub
  :save-state
  (fn [{::db/keys [pending-saves processing-saves save-errors]}]
    (cond
      ; if there are any processing, show :saving state
      (seq processing-saves) :saving

      ; nothing processing, but some pending
      (seq pending-saves) :pending

      ; idle, but something went wrong
      (seq save-errors) :error

      ; otherwise, idle
      :else :idle)))


; ======= Push notifications ==============================

(reg-sub
  :interested-push-ids
  :<- [:active-sheet-id]
  :<- [:active-sheet-source-ids]
  :<- [:my-sheets]
  :<- [:meta/players]
  (fn [[sheet-id source-ids my-sheets campaign-players] _]
    (cond
      ; in campaign mode we're interested in all the player sheets, and
      ; all of our sources. In the future, we might want to get all the
      ; sources of all the player sheets. We could potentially also
      ; subscribe to the campaign ID for real-time communication from
      ; players
      campaign-players
      (into campaign-players
            source-ids)

      ; NOTE: currently, we're only interested in changes to sheets we
      ; *don't* own.
      ; We could later listen to changes in the sources for a sheet we
      ; own, in which case we wouldn't be interested in sheet-id
      (and sheet-id
           (not (contains? my-sheets sheet-id)))
      (into #{sheet-id} source-ids)

      :else nil)))
