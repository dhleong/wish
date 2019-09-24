(ns ^{:author "Daniel Leong"
      :doc "dnd5e.subs"}
  wish.sheets.dnd5e.subs
  (:require-macros [wish.util.log :as log])
  (:require [clojure.string :as str]
            [re-frame.core :as rf :refer [reg-sub subscribe]]
            [wish-engine.core :as engine]
            [wish.sheets.dnd5e.data :as data]
            [wish.sheets.dnd5e.util :as util :refer [ability->mod ->die-use-kw]]
            [wish.sheets.dnd5e.builder.data :refer [point-buy-max
                                                    score-point-cost]]
            [wish.sheets.dnd5e.subs.base]
            [wish.sheets.dnd5e.subs.abilities :as abilities]
            [wish.sheets.dnd5e.subs.spells :as spells]
            [wish.sheets.dnd5e.subs.util
             :refer [filter-by-str feature-by-id feature-in-lists]]
            [wish.subs-util :refer [reg-id-sub query-vec->preferred-id]]
            [wish.util :refer [<sub invoke-callable ->map]]))



; ======= utils ============================================

(defn- reg-sheet-sub
  "Convenience for creating a sub that just gets a specific
   field from the :sheet key of the sheet-meta"
  [id getter]
  (reg-id-sub
    id
    :<- [:meta/sheet]
    (fn [sheet _]
      (getter sheet))))



; ======= utility subs ====================================

(defn- compute-buff [entity buff-entry]
  (if (fn? buff-entry)
    (buff-entry entity)
    buff-entry))

(defn- compute-buffs [entity buffs-map]
  (reduce (fn [total b]
            (+ total (compute-buff entity b)))
          0
          (vals buffs-map)))

; the ::buffs sub takes a single :buff type ID (not including an ability,
; since some buffs depend on ability modifiers) and computes and combines
; all attributed buffs across classes and races
(reg-id-sub
  ::buffs
  :<- [:effect-ids-set]
  :<- [::abilities/modifiers]
  :<- [:total-level]
  :<- [::base-speed]
  :<- [:races]
  :<- [:classes]
  :<- [::attuned-eq]
  :<- [:effects]
  (fn [[effects-set modifiers total-level base-speed races & entity-lists]
       [_ & buff-path]]
    (let [full-buff-path (into [:attrs :buffs] buff-path)]
      (->> entity-lists

           ; NOTE some racial abilities buff based on the total class level
           (concat (map #(assoc % :level total-level) races))

           flatten

           (reduce
             (fn [^number total-buff entity]
               (+ total-buff
                  (let [buffs (get-in entity full-buff-path)]
                    (cond
                      (nil? buffs) 0
                      (number? buffs) buffs
                      (map? buffs) (compute-buffs
                                     (assoc entity
                                            ; hopefully there are few other things
                                            ; that can be doubled...
                                            :speed (+ base-speed
                                                      (when (= buff-path [:speed])
                                                        total-buff))
                                            :effects effects-set
                                            :modifiers modifiers)
                                     buffs)

                      :else (throw (js/Error.
                                     (str "Unexpected buffs value for "
                                          buff-path
                                          ": " (type buffs)
                                          " -> `" buffs "`")))))))
             0)))))

(reg-id-sub
  ::buff-attrs
  :<- [:all-attrs]
  (fn [attrs [_ buff-id]]
    (get-in attrs [:buffs buff-id])))

; ======= effects =========================================

(reg-sub
  :5e/effects-filter
  (fn [db]
    (:5e/effects-filter db nil)))

; returns a map of id -> {buff-id -> n}
(reg-sub
  ::effect-buffs-map
  :<- [:effects]
  (fn [effects _]
    (->> effects
         (map (comp :buffs :attrs))
         (apply merge-with merge))))

(reg-sub
  ::effect-buffs-values-map
  :<- [::effect-buffs-map]
  :<- [::abilities/modifiers]
  :<- [::base-speed]
  (fn [[effects modifiers speed] _]
    ; NOTE: for now it is okay that we don't necessarily get
    ; the complete buff value for speed...
    (let [entity {:modifiers modifiers
                  :speed speed}]
      (reduce-kv
        (fn [m effect-id buffs-map]
          (assoc m effect-id
                 (compute-buffs
                   entity
                   buffs-map)))
        {}
        effects))))

; :buff, :nerf, or nil for the given ID
(reg-sub
  ::effect-change-for
  :<- [::effect-buffs-values-map]
  (fn [buffs-map [_ id]]
    (when-let [value (get buffs-map id)]
      (cond
        (> value 0) :buff
        (< value 0) :nerf
        :else nil))))

(reg-sub
  ::all-effects
  :<- [:all-effects/sorted]
  :<- [:effect-ids-set]
  :<- [:5e/effects-filter]
  (fn [[items active-ids filter-str]]
    (->> items
         (remove :feature-only?)
         (remove (comp active-ids :id))
         (filter-by-str filter-str))))


; ======= class and level ==================================

(reg-id-sub
  ::class->level
  :<- [:classes]
  (fn [classes _]
    (reduce
      (fn [m c]
        (assoc m (:id c) (:level c)))
      {}
      classes)))

(reg-id-sub
  ::class-level
  :<- [::class->level]
  (fn [classes [_ ?sheet-id ?class-id]]
    ; NOTE: when called normally, ?sheet-id is actually the class-id.
    ; when called as an id-sub, we use ?class-id
    (get classes (or ?class-id
                     ?sheet-id))))


(reg-id-sub
  ::limited-use-configs
  :<- [:all-limited-use-configs]
  :<- [:total-level]
  :<- [::abilities/modifiers]
  :<- [::attuned-ids]
  (fn [[items total-level modifiers attuned-set]]
    (->> items
         (remove :implicit?)

         ; eagerly evaluate :uses (the sheet shouldn't do this)
         (map (fn [limited-use]
                (update limited-use
                        :uses
                        (fn [value]
                          (if (ifn? value)
                            (invoke-callable limited-use :uses
                                             :modifiers modifiers
                                             :total-level total-level)
                            value)))))

         ; remove uses that come from un-attuned items that require attunement
         (remove (fn [item]
                   (and (= :item (:wish/context-type item))
                        (:attunes? (:wish/context item))
                        (not (contains? attuned-set (:id (:wish/context item)))))))

         (sort-by :name))))

(reg-sub
  ::limited-use
  :<- [::limited-use-configs]
  :<- [:limited-used]
  (fn [[items used] [_ id]]
    (->> items
         (filter #(= id (:id %)))
         (map #(assoc % :uses-left (- (:uses %)
                                      (get used id))))
         first)))

; Takes an entity with :consumes and returns something
; that can be consumed from it. Usually this delegates to
; [::limited-use (:consumes a)], but this also supports
; the special case of consuming a :*spell-slot
(reg-sub
  ::consumable
  (fn [[_ {id :consumes :as entity}]]
    (if (not= :*spell-slot id)
      (subscribe [::limited-use id])

      ; special case
      (subscribe [::spells/usable-slot-for entity])))
  (fn [input [_ {id :consumes}]]
    (if (not= :*spell-slot id)
      ; easy case
      input

      {:id :*spell-slot
       :name (str (get data/level-suffixed (:level input))
                  "-level Spell Slot")
       :uses-left (:unused input)
       :slot-kind (:kind input)
       :slot-level (:level input)
       :max-slots (:total input)}
      )))

(reg-id-sub
  ::rolled-hp
  :<- [:meta/sheet]
  (fn [sheet [_ ?path]]
    (get-in sheet (concat
                    [:hp-rolled]

                    ; NOTE: as an id-sub, we can also be called
                    ; where the var at this position is the sheet id
                    (when (coll? ?path)
                      ?path)))))

(reg-sheet-sub
  ::temp-hp
  :temp-hp)

(reg-sheet-sub
  ::temp-max-hp
  :temp-max-hp)

(reg-id-sub
  ::max-hp-mode
  :<- [:meta/sheet]
  (fn [sheet]
    (or (:max-hp-mode sheet)

        ; if not specified and they have any rolled, use that
        (when (:hp-rolled sheet)
          :manual)

        ; default to :average for new users
        :average)))

(reg-id-sub
  ::max-hp-rolled
  :<- [::rolled-hp]
  :<- [::class->level]
  (fn [[rolled-hp class->level]]
    (->> rolled-hp

         ; if you set a class to level 3, set HP, then go back
         ; to level 2, there will be an orphaned entry in the
         ; rolled-hp vector. We could remove that entry when
         ; changing the level, but accounting for it here means
         ; that an accidental level-down doesn't lose your data
         ; Also, if you've removed a class that you once rolled HP
         ; for, we don't care about that class's old, rolled hp
         (reduce-kv
           (fn [all-entries class-id class-rolled-hp]
             (if-let [class-level (class->level class-id)]
               (concat all-entries
                       (take class-level
                             class-rolled-hp))

               ; no change
               all-entries))
           nil)

         (apply +))))

(reg-id-sub
  ::max-hp-average
  :<- [:classes]
  (fn [classes]
    (reduce
      (fn [total c]
        (let [{:keys [primary? level]
               {hit-die :5e/hit-dice} :attrs} c

              ; the primary class gets full HP at first level,
              ; so remove one from it (we add this special case below)
              level (if primary?
                      (dec level)
                      level)]
          (+ total

             (when primary?
               hit-die)

             (* level
                (inc (/ hit-die 2))))))

      0 ; start at 0
      classes)))

(reg-id-sub
  ::max-hp
  (fn [query-vec]
    [; NOTE: this <sub is kinda gross but I *think* it's okay?
     ; subscriptions are de-dup'd so...?
     ; The only other way would be to always subscribe to both,
     ; and that seems worse
     (case (<sub [::max-hp-mode (query-vec->preferred-id query-vec)])
       :manual [::max-hp-rolled]
       :average [::max-hp-average])

     [::temp-max-hp]
     [::abilities/all]
     [:total-level]
     [::buffs :hp-max]
     ])
  (fn [[base-max temp-max abilities total-level buffs]]
    (+ base-max

       temp-max

       (* total-level
          (->> abilities
               :con
               ability->mod))

       buffs)))

(reg-id-sub
  ::hp
  :<- [::temp-hp]
  :<- [::max-hp]
  :<- [:limited-used]
  (fn [[temp-hp max-hp limited-used-map]]
    (let [used-hp (or (:hp#uses limited-used-map)
                      0)]
      [(+ temp-hp
          (- max-hp used-hp)) max-hp])))

(reg-sheet-sub
  ::death-saving-throws
  :death-saving-throws)


; ======= Proficiency and expertise ========================


(def ^:private static-resistances
  #{:acid :cold :fire :lightning :poison})

; returns a collection of features
(reg-sub
  ::ability-extras
  :<- [:sheet-engine-state]
  :<- [:races]
  :<- [:classes]
  :<- [::attuned-eq]
  :<- [:effects]
  (fn [[data-source & entity-lists] _]
    (->> entity-lists
         flatten
         (mapcat (comp
                   (partial apply concat)
                   (juxt (comp :saves :attrs)
                         (comp :immunities :attrs)
                         (comp :resistances :attrs))))

         ; TODO include the source?
         (map (fn [[id extra]]
                (cond
                  (true? extra)
                  (if (contains? static-resistances id)
                    ; static
                    {:id id
                     :desc (str "You are resistant to "
                                (str/capitalize (name id))
                                " damage.")}

                    ; not static? okay, it could be a feature
                    (if-let [f (feature-in-lists data-source entity-lists id)]
                      ; got it!
                      f

                      ; okay... effect?
                      (if-let [e (get-in data-source [:effects id])]
                        {:id id
                         :desc [:<>
                                (:name e) ":"
                                [:ul
                                 (for [line (:effects e)]
                                   ^{:key line}
                                   [:li line])]]}

                        ; halp
                        {:id id
                         :desc (str "Unknown: " id " / " extra)})))

                  ; full feature
                  (:id extra)
                  extra

                  ; shorthand (eg: just {:desc}):
                  :else
                  (assoc extra :id id)))))))

(reg-sub
  ::other-proficiencies
  :<- [:sheet-engine-state]
  :<- [::all-proficiencies]
  (fn [[data-source feature-ids] _]
    (->> feature-ids
         (remove data/skill-feature-ids)
         (keep (partial feature-by-id data-source))
         (sort-by :name))))

; returns a collection of feature ids
(reg-sub
  ::languages
  :<- [:sheet-engine-state]
  :<- [:races]
  :<- [:classes]
  :<- [:effects]
  (fn [[data-source & entity-lists] _]
    (->> entity-lists
         flatten
         (mapcat :attrs)
         (keep (fn [[k v]]
                 (when (and v
                            (= "lang" (namespace k)))
                   k)))
         (keep (partial feature-by-id data-source))
         (sort-by :name))))


; ======= general stats for header =========================

(reg-sub
  ::passive-perception
  :<- [::abilities/modifiers]
  :<- [::proficiency-bonus]
  :<- [::save-proficiencies]
  (fn [[abilities prof-bonus save-profs]]
    (+ 10
       (:wis abilities)
       (when (:wis save-profs)
         prof-bonus))))

(reg-sub
  ::base-speed
  :<- [:race]
  (fn [race]
    (-> race :attrs :5e/speed)))

(reg-sub
  ::speed
  :<- [::base-speed]
  :<- [::buffs :speed]
  (fn [[base buffs]]
    (+ base buffs)))


; ======= items and equipment ==============================

(reg-sub
  ::equipped-sorted
  :<- [:equipped-sorted]
  (fn [equipped]
    (map (fn [item]
           (case (:type item)
             :armor (data/inflate-armor item)
             :weapon (data/inflate-weapon item)
             item))
         equipped)))


(reg-sub
  :5e/items-filter
  (fn [db]
    (:5e/items-filter db nil)))

(reg-sub
  ::all-items
  :<- [:all-items]
  :<- [:5e/items-filter]
  (fn [[items filter-str]]
    (filter-by-str filter-str items)))

(reg-sheet-sub
  ::attuned-ids
  :attuned)

; all equipped items that are attuned (or that don't need to be attuned)
(reg-sub
  ::attuned-eq
  :<- [::equipped-sorted]
  :<- [::attuned-ids]
  (fn [[equipped attuned-set]]
    (->> equipped
         (remove (fn [item]
                   (and (:attunes? item)
                        (not (contains? attuned-set (:id item)))))))))

; returns a map of :kinds and :categories
(reg-sub
  ::eq-proficiencies
  :<- [:classes]
  :<- [:races]
  (fn [entity-lists]
    (->> entity-lists
         flatten
         (map :attrs)
         (reduce
           (fn [m attrs]
             (-> m
                 (update :kinds conj (:weapon-kinds attrs))
                 (update :categories conj (:weapon-categories attrs))))
           {:kinds {}
            :categories {}}))))

; like :inventory-sorted but with :attuned? added as appropriate
(reg-sub
  ::inventory-sorted
  :<- [:inventory-sorted]
  :<- [::attuned-ids]
  (fn [[inventory attuned-set]]
    (->> inventory
         (map (fn [item]
                (if (contains? attuned-set (:id item))
                  (assoc item :attuned? true)
                  item))))))

; current quantity of the given item
(reg-sub
  ::item-quantity
  :<- [:inventory-map]
  (fn [m [_ item-id]]
    (->> m item-id :wish/amount)))


; ======= builder-specific =================================

(reg-sub
  ::abilities-mode
  :<- [:meta/sheet]
  (fn [sheet]
    (or
      ; normal case
      (:abilities-mode sheet)

      ; legacy compat
      (when-let [existing (->> sheet :abilities vals seq)]
        (when (not= #{8 10 12 13 14 15}
                    (set existing))
          :manual))

      ; default for new characters
      :standard)))

(defn calculate-scores-cost
  "Given a map of abilities, calculate how many points it costs"
  [abilities]
  (->> abilities
       vals
       (map score-point-cost)
       (apply +)))

; number of "points" remaining when using the point-buy system
; of ability score generation
(reg-sub
  ::point-buy-remaining
  :<- [::abilities/raw]
  (fn [abilities]
    (- point-buy-max
       (calculate-scores-cost abilities))))

(reg-sub
  ::point-buy-delta
  :<- [::abilities/raw]
  (fn [abilities [_ ability new-cost]]
    (let [current-cost (->> abilities
                            ability
                            (get score-point-cost))]
      (- current-cost
         new-cost))))

(defn- multiclass-error
  "Returns nil if the given class `c` can be multiclassed into,
   else a String explanation of the ability prereqs that weren't met"
  [c abilities]
  (when-let [get-error (get-in c [:attrs :5e/multiclass-reqs])]
    (get-error abilities)))

(defn- available-classes
  [all-classes selected-classes primary-class abilities]
  (let [first-class? (empty? selected-classes)
        ; there can't be a multiclass error for the first class
        primary-multiclass-error (when-not first-class?
                                   (multiclass-error
                                     primary-class
                                     abilities))
        selected-class-ids (->> selected-classes
                                (map :id)
                                (into #{}))]
    (->> all-classes
         (remove (comp selected-class-ids :id))
         (map
           (fn [c]
             (if
               ; if the primary can't multiclass,
               ; nobody can!
               primary-multiclass-error
               (assoc c :prereqs-failed? true
                      :prereqs-reason (str "Starting class does not meet multiclass prerequisites: "
                                           primary-multiclass-error))

               ; if primary is good, then this class's reqs must also be satisfied
               (if-let [err (when-not first-class?
                              (multiclass-error
                                c
                                abilities))]
                 (assoc c :prereqs-failed? true
                        :prereqs-reason (str "Multiclass prerequisites not met: "
                                             err))

                 ; good to go!
                 c))
             ))
         (sort-by :name))))

(reg-sub
  ::available-classes
  :<- [:available-entities :classes]
  :<- [:classes]
  :<- [::primary-class]
  :<- [::abilities/base]
  (fn [[all-classes selected-classes primary-class abilities]]
    (available-classes
      (map util/prepare-class-for-builder all-classes)
      selected-classes
      (util/prepare-class-for-builder primary-class)
      abilities)))

(reg-sub
  ::available-races
  :<- [:available-entities :races]
  (fn [all-races]
    (->> all-races
         (sort-by (fn [e]
                    [(or (:subrace-of e)
                         (:id e))
                     (:subrace-of e)
                     (:name e)])))))

(reg-sub
  ::primary-class
  :<- [:classes]
  (fn [classes]
    (->> classes
         (filter :primary?)
         first)))

(reg-sub
  ::class-by-id
  :<- [:classes]
  (fn [classes [_ id]]
    (->> classes
         (filter #(= id (:id %)))
         first)))

(reg-sub
  ::background
  :<- [:race-features-with-options]
  (fn [features]
    (->> features
         (filter #(= :background (first %))))))

(def ^:private custom-background-feature-ids
  #{:custom-bg/skill-proficiencies
    :custom-bg/feature
    :custom-bg/tools-or-languages})
(reg-sub
  ::custom-background
  :<- [:race-features-with-options]
  (fn [features]
    (->> features
         (filter #(custom-background-feature-ids (first %))))))

; like the default one, but removing :background
(reg-sub
  ::race-features-with-options
  :<- [:race-features-with-options]
  (fn [features]
    (->> features
         (remove #(= :background (first %)))
         (remove #(custom-background-feature-ids (first %))))))

; like the default one, but providing special handling for :all-spells
(reg-sub
  ::class-features-with-options
  (fn [[_ entity-id]]
    [(subscribe [:class-features-with-options entity-id])
     (subscribe [::spells/highest-spell-level-for-spellcaster-id entity-id])])
  (fn [[features highest-spell-level]]
    (->> features
         (map (fn [[id f :as entry]]
                (if (= [:all-spells]
                       (:wish/raw-values f))
                  ; when a feature lets you pick *any* spell, it's always
                  ; limited to spells you can actually cast
                  [id (update f :values
                              (partial filter
                                       #(<= (:spell-level %)
                                            highest-spell-level)))]

                  ; normal case
                  entry))))))

(reg-sub
  ::available-feature-options
  (fn [[_ source-sub _feature-id instance-id]]
    [(subscribe source-sub)
     (subscribe [:options-> (if (seq? instance-id)
                              instance-id
                              [instance-id])])])
  (fn [[features options] [_ _ feature-id _instance-id]]
    ; options are available either if we have it selected,
    ; or if it is explicitly available. This way, feature options
    ; that are shared across features but which should not be
    ; available more than once can mark themselves as unavailable
    ; when chosen, and still be seen in the character builder
    (->> features
         (filter #(= feature-id (first %)))
         first  ; first (only) match
         second ; the feature
         :values

         (filter :available?)
         (map :id)

         (concat options)
         (into #{}))))

; returns a bit more than just the hit die, for convenience
(reg-sub
  ::class->hit-die
  :<- [:classes]
  (fn [classes]
    (reduce
      (fn [m c]
        (assoc m (:id c)
               (assoc
                 (select-keys c [:name :level])
                 :dice (-> c :attrs :5e/hit-dice))))
      {}
      classes)))


; ======= starting equipment ==============================

(reg-sub
  ::starter-packs-by-id
  :<- [:sheet-engine-state]
  (fn [source]
    (->map
      (engine/inflate-list source :5e/starter-packs))))

(defn- select-filter-keys
  "Like (select-keys) but any keys that weren't missing
   get a default value of false"
  [item keyseq]
  (reduce
    (fn [m k]
      (assoc m k (get item k false)))
    {}
    keyseq))

(defmulti unpack-eq-choices (fn [_source _packs choices]
                              (cond
                                (vector? choices) :and
                                (list? choices) :or
                                (map? choices) :filter
                                (keyword? choices) :id
                                :else
                                (do
                                  (log/warn "Unexpected choices: " choices)
                                  (type choices)))))
(defmethod unpack-eq-choices :or
  [source packs choices]
  [:or (map (partial unpack-eq-choices source packs) choices)])
(defmethod unpack-eq-choices :and
  [source packs choices]
  [:and (map (partial unpack-eq-choices source packs) choices)])
(defmethod unpack-eq-choices :filter
  [source _ choices]
  (if-let [id (:id choices)]
    ; single item with a :count
    [:count (get-in source [:items id]) (:count choices)]

    ; filter
    (let [choice-keys (keys choices)]
      [:or (->> source :items vals  ; all items
                (remove :+) ; no magic items
                (remove :desc) ; or fancy items
                (filter (fn [item]
                          ; would it be more efficient to just make a
                          ; custom = fn here?
                          (let [matching-keys (select-filter-keys item choice-keys)]
                            (= matching-keys choices)))))])))
(defmethod unpack-eq-choices :id
  [source packs choice]
  (or (when-let [p (get packs choice)]
        ; packs are special
        [:pack (update p :contents
                       (partial
                         map
                         (fn [[id amount]]
                           [(get-in source [:items id])
                            amount])))])

      ; just an item
      (get-in source [:items choice])))

(reg-sub
  ::starting-eq
  :<- [:sheet-engine-state]
  :<- [::starter-packs-by-id]
  :<- [:primary-class]
  (fn [[source packs {{eq :5e/starting-eq} :attrs
                      :as primary-class}]]
    {:class primary-class
     :choices
     (map
       (partial unpack-eq-choices source packs)
       eq)}))


; ======= etc ==============================================

(reg-sheet-sub
  ::currency
  :currency)

(reg-sheet-sub
  ::conditions
  :conditions)

(reg-sub
  ::selected-option-ids
  :<- [:meta/options]
  (fn [options]
    (->> options
         vals
         flatten
         set)))

; hacks?
(reg-sub
  ::features-for
  (fn [[_ sub-vec]]
    [(subscribe sub-vec)
     (subscribe [:meta/options])
     (subscribe [::selected-option-ids])])
  (fn [[sources options selected-options]]
    (->> sources
         ; the sub-vec :class-features or :race-features
         ; returns a seq of map entries; we just want the values
         (map second)

         (filter :name)
         (remove :implicit?)

         (map (fn [f]
                (if-let [inst-id (:wish/instance-id f)]
                  (assoc f :id inst-id)
                  f)))

         ; if the feature is an option, we only want it
         ; if it was actually selected
         (remove #(and (:wish/option? %)
                       (not (contains? selected-options
                                       (:id %)))))

         ; filter out un-selected values
         (map (fn [f]
                (if-let [chosen (get options (:id f))]
                  (let [chosen (if (map? chosen)
                                 ; instanced feature
                                 (:value chosen)
                                 chosen)]
                    (update f :values
                            (partial filter
                                     #(some #{(:id %)} chosen))))

                  (dissoc f :values))))

         (sort-by :name)
         seq)))

; returns a list of {:die,:classes,:used,:total}
; where :classes is a list of class names, sorted by die size.
(reg-sub
  ::hit-dice
  :<- [:classes]
  :<- [:limited-used]
  (fn [[classes used]]
    (->> classes
         (reduce
           (fn [m c]
             (let [die-size (-> c :attrs :5e/hit-dice)]
               (if (get m die-size)
                 ; just add our class and inc the total
                 (-> m
                     (update-in [die-size :classes] conj (:name c))
                     (update-in [die-size :total] + (:level c)))

                 ; create the initial spec
                 (assoc m die-size {:classes [(:name c)]
                                    :die die-size
                                    :used (get used (->die-use-kw die-size))
                                    :total (:level c)}))))
           {})
         vals
         (sort-by :die #(compare %2 %1)))))

(reg-sheet-sub
  ::notes
  :notes)
