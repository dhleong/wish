(ns wish.subs
  (:require-macros [wish.util.log :as log])
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub subscribe]]
            [wish.db :as db]
            [wish.inventory :as inv]
            [wish.subs-util :refer [active-sheet-id]]
            [wish.sheets :as sheets]
            [wish.sources.compiler :refer [apply-directives inflate]]
            [wish.sources.compiler.lists :as lists]
            [wish.sources.core :as src :refer [find-class find-race]]))

(reg-sub :showing-overlay :showing-overlay)

; ======= Provider-related =================================

(reg-sub :provider-states :provider-states)
(reg-sub
  :provider-state
  :<- [:provider-states]
  (fn [states [_ provider-id]]
    (when-not provider-id
      (js/console.warn "[subs] nil provider-id provided to :provider-state"))
    (get states provider-id :idle)))

(reg-sub
  :providers-listing?
  (fn [db _]
    (seq (:providers-listing db))))


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
  (reg-sub
    id
    :<- [:sheet-meta]
    (fn [sheet _]
      (getter sheet))))

(reg-sub :page :page)
(reg-sub :sheets :sheets)
(reg-sub :sheet-sources :sheet-sources)

(reg-meta-sub :meta/sheet :sheet)
(reg-meta-sub :meta/sources :sources)
(reg-meta-sub :meta/kind :kind)
(reg-meta-sub :meta/classes (comp vals :classes))
(reg-meta-sub :meta/races :races)
(reg-meta-sub :limited-used :limited-uses)
(reg-meta-sub :meta/options :options)
(reg-meta-sub :meta/inventory :inventory)
(reg-meta-sub :meta/items :items)
(reg-meta-sub :meta/equipped :equipped)

(reg-sub
  :active-sheet-source-ids
  :<- [:meta/sources]
  set)

(reg-sub
  :active-sheet-id
  :<- [:page]
  (fn [page-vec _]
    (active-sheet-id nil page-vec)))

(reg-sub
  :provided-sheet
  :<- [:sheets]
  (fn [sheets [_ sheet-id]]
    (assoc (get sheets sheet-id)
           :id sheet-id)))

(reg-sub
  :known-sheets
  :<- [:sheets]
  (fn [sheets _]
    (->> sheets
         (map (fn [[id v]]
                (assoc v :id id)))
         (filter :name)
         (sort-by :name))))

(reg-sub
  :my-known-sheets
  :<- [:known-sheets]
  (fn [sheets _]
    (->> sheets
         (filter :mine?))))

(reg-sub
  :shared-known-sheets
  :<- [:known-sheets]
  (fn [sheets _]
    (->> sheets
         (remove :mine?))))


; if a specific sheet-id is not provided, loads
; for the active sheet id
(reg-sub
  :sheet-source
  :<- [:sheet-sources]
  :<- [:active-sheet-id]
  (fn [[sources active-id] [_ sheet-id]]
    (let [{:keys [source loaded?]} (get sources (or sheet-id
                                                    active-id))]
      (when loaded?
        source))))

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

(reg-sub
  :sheet-meta
  :<- [:sheets]
  :<- [:active-sheet-id]
  (fn [[sheets id]]
    (get sheets id)))

(reg-sub
  :classes
  :<- [:meta/kind]
  :<- [:sheet-source]
  :<- [:meta/options]
  :<- [:meta/classes]
  (fn [[sheet-kind source options metas] _]
    (when source
      (->> metas
           (map (fn [m]
                  (merge m (find-class source (:id m)))))
           (map (fn [c]
                  (-> c
                      (inflate source options)
                      (sheets/post-process
                        sheet-kind
                        source
                        :class)))))
      )))

; A single class instance, or nil if none at all; if any
; class is marked primary, that class is returned. If none
; are so marked, then nil is returned
(reg-sub
  :primary-class
  :<- [:classes]
  (fn [classes]
    (->> classes
         (filter :primary?)
         first)))

; sum of levels from all classes
(reg-sub
  :total-level
  :<- [:classes]
  (fn [classes _]
    (apply + (map :level classes))))

(reg-sub
  :races
  :<- [:sheet-meta]
  :<- [:sheet-source]
  :<- [:meta/options]
  :<- [:total-level]
  :<- [:meta/races]
  (fn [[sheet-meta source options total-level ids] _]
    (when source
      (->> ids
           (map (partial find-race source))
           (map (fn [r]
                  (-> r
                      ; provide :level for level-scaling...
                      (assoc :level total-level)

                      (inflate source options)

                      ; ... then remove
                      (dissoc :level)

                      (sheets/post-process
                        (:kind sheet-meta)
                        source
                        :race))))))))

(defn- get-features
  "Returns a collection of [id feature] pairs."
  [feature-containers [_ entity-id primary?]]
  (->> (if entity-id
         (filter #(= entity-id (:id %)) feature-containers)
         feature-containers)

       (mapcat (fn [container]
                 (map (fn [f]
                        (with-meta
                          f
                          {:wish/container-id (:id container)
                           :wish/container container}))
                      (:features container))))

       ; remove features that only the primary class should have
       ; if we're not the primary
       (remove #(when (:primary-only? (second %))
                  (not primary?)))

       ; expand multi-instanced features
       (mapcat (fn [[id f :as entry]]
                 (if (:instanced? f)
                   (let [total-instances (:wish/instances f 1)
                         {:wish/keys [container-id]} (meta entry)]
                     (map
                       (fn [n]
                         (-> entry
                             (assoc-in
                               [1 :wish/instance-id]
                               (keyword
                                 (namespace id)
                                 (str (name id)
                                      "#"
                                      (name container-id)
                                      "#"
                                      n)))
                             (assoc-in [1 :wish/instance] n)))
                       (range total-instances)))

                   ; normal feature
                   [entry])))))

(defn inflate-option-values
  [data-source options feature-id values]
  (or (when-let [feature-values
                 (when data-source
                   (:values
                     (src/find-feature data-source feature-id)))]
        (when-not (and (= feature-values values)
                       (some keyword? values))
          ; inflated values from a feature
          feature-values))

      ; not a feature with :values? Okay inflate now
      (mapcat
        (fn [opt-or-id]
          (if (keyword? opt-or-id)
            (or (when-let [f (src/find-feature data-source opt-or-id)]
                  [f])
                (src/expand-list data-source opt-or-id nil)

                (when-let [f (src/find-list-entity data-source opt-or-id)]
                  [f])

                (when-let [options-src-id (lists/unpack-options-key
                                            opt-or-id)]
                  ; this was eg: :wizard/spells-list>>options
                  ; inflate the chosen :values from the given feature-id
                  (inflate-option-values
                    data-source
                    options
                    options-src-id
                    (get options options-src-id)))

                (log/warn "Unable to inflate  " opt-or-id))

            ; provided value; wrap in collection so the maps' entries
            ; don't get flattened by mapcat
            [opt-or-id]))
        values)))

(defn- filter-available
  "Updates all options, evaluating :available? 'in place'
   if provided, or setting to `true` if not. What we would
   prefer to do is actually (filter) the elements, but because
   reagent-forms doesn't handle a dynamically changing set of
   options for a :list, we set this flag so it can be later
   queried from a :visible? function"
  [available-map values]
  ;; (filter
  ;;   (fn [v]
  ;;     (if-let [available? (:available? v)]
  ;;       (available? available-map)
  ;;
  ;;       ; if not provided, it's always available
  ;;       true))
  ;;   values)
  (map
    (fn [v]
      (assoc v :available?
             (if-let [available? (:available? v)]
               (available? available-map)

               ; if not provided, it's always available
               true)))
    values)
  )

(defn- inflate-feature-options
  [[features options sheet data-source]]
  (->> features
       (map (fn [[id v :as entry]]
              (let [container (-> entry meta :wish/container)
                    available-map (assoc container
                                         :options options
                                         :sheet sheet)]
                (with-meta
                  [id (-> v
                          (assoc :wish/raw-values (:values v))
                          (update :values (comp
                                            (partial map
                                                     (fn [v]
                                                       (assoc v :level (:level container))))
                                            (partial filter-available
                                                     available-map)
                                            (partial inflate-option-values
                                                     data-source
                                                     options
                                                     id)))

                          ; filter values, if a fn was provided
                          (as-> v
                            (if-let [filter-fn (:values-filter v)]
                              (update v :values (partial filter filter-fn))
                              v))
                          )]
                  (meta entry)))))))

(defn- only-feature-options
  [[features options sheet data-source]]
  (inflate-feature-options
    [(filter (comp :max-options second) features)
     options
     sheet
     data-source]))

(reg-sub
  :class-features
  :<- [:classes]
  get-features)

(reg-sub
  :inflated-class-features
  (fn [[_ entity-id primary?]]
    [(subscribe [:class-features entity-id primary?])
     (subscribe [:meta/options])
     (subscribe [:meta/sheet])
     (subscribe [:sheet-source])])
  inflate-feature-options)

; like :inflated-class-features but removing features
; that don't accept options
(reg-sub
  :class-features-with-options
  (fn [[_ entity-id primary?]]
    [(subscribe [:class-features entity-id primary?])
     (subscribe [:meta/options])
     (subscribe [:meta/sheet])
     (subscribe [:sheet-source])])
  only-feature-options)

(reg-sub
  :race-features
  :<- [:races]
  get-features)

(reg-sub
  :inflated-race-features
  :<- [:race-features]
  :<- [:meta/options]
  :<- [:meta/sheet]
  :<- [:sheet-source]
  inflate-feature-options)

(reg-sub
  :race-features-with-options
  :<- [:race-features]
  :<- [:meta/options]
  :<- [:meta/sheet]
  :<- [:sheet-source]
  only-feature-options)

; semantic convenience for single-race systems
(reg-sub
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

(reg-sub
  :limited-uses
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

(reg-sub
  :limited-uses-map
  :<- [:limited-uses]
  (fn [limited-uses]
    (reduce
      (fn [m v]
        (assoc m (:id v) v))
      {}
      limited-uses)))

(defn inflate-item
  "Given the character's :items map and datasource,
   return the inflated item for the given inst-id."
  [inst-id items data-source]
  (let [item (get items inst-id)
        item-id (if-not item
                  ; no items entry? must be an item-id already
                  inst-id

                  ; get the :id from the item entry, if any
                  (get item :id))]
    (merge
      (dissoc item :id)
      (apply-directives
        (or (when item-id
              (src/find-item data-source item-id))
            item)
        data-source))))

; map of :inst-id -> inflated item in the active sheet's inventory,
; where each inflated item with an amount > 1 (or which :stacks?)
; includes the special key :wish/amount indicating that amount. The
; :id of each item will always be the instance id, and the :item-id
; will always be the (surprise) item-id.
; In addition, every item in :equipped will have the :wish/equipped?
; set to true
(reg-sub
  :inventory-map
  :<- [:meta/kind]
  :<- [:meta/inventory]
  :<- [:meta/items]
  :<- [:meta/equipped]
  :<- [:sheet-source]
  (fn [[sheet-kind raw-inventory items equipped data-source]]
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
                         (sheets/post-process
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
(reg-sub
  :inventory-sorted
  :<- [:inventory-map]
  (fn [inventory-map]
    (->> inventory-map
         vals
         (sort-by :name))))

; sorted list of inflated + equipped inventory items
(reg-sub
  :equipped-sorted
  :<- [:inventory-sorted]
  (fn [inventory-sorted]
    (->> inventory-sorted
         (filter :wish/equipped?))))


; list of all known items for the current sheet
(reg-sub
  :all-items
  :<- [:meta/kind]
  :<- [:sheet-source]
  (fn [[sheet-kind source]]
    (->> (src/list-entities source :items)
         (map #(sheets/post-process
                 %
                 sheet-kind
                 source
                 :item))
         (sort-by :name))))


; ======= character builder-related ========================

(reg-sub
  :available-entities
  :<- [:sheet-source]
  (fn [source [_ entity-kind]]
    (src/list-entities source entity-kind)))


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
      (not (empty? processing-saves)) :saving

      ; nothing processing, but some pending
      (not (empty? pending-saves)) :pending

      ; idle, but something went wrong
      (not (empty? save-errors)) :error

      ; otherwise, idle
      :else :idle)))
