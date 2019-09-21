(ns ^{:author "Daniel Leong"
      :doc "dnd5e.subs"}
  wish.sheets.dnd5e.subs
  (:require-macros [wish.util.log :as log])
  (:require [clojure.string :as str]
            [re-frame.core :as rf :refer [reg-sub subscribe]]
            [wish-engine.core :as engine]
            [wish.sources.util :as src-util]
            [wish.sheets.dnd5e.data :as data]
            [wish.sheets.dnd5e.util :as util :refer [ability->mod ->die-use-kw
                                                     mod->str]]
            [wish.sheets.dnd5e.builder.data :refer [point-buy-max
                                                    score-point-cost]]
            [wish.subs-util :refer [reg-id-sub query-vec->preferred-id]]
            [wish.util :refer [<sub invoke-callable ->map ->set]]
            [wish.util.string :as wstr]))

; ======= Constants ========================================

(def spell-slot-schedules
  {:standard
   {1 {1 2}
    2 {1 3}
    3 {1 4, 2 2}
    4 {1 4, 2 3}
    5 {1 4, 2 3, 3 2}
    6 {1 4, 2 3, 3 3}
    7 {1 4, 2 3, 3 3, 4 1}
    8 {1 4, 2 3, 3 3, 4 2}
    9 {1 4, 2 3, 3 3, 4 3, 5 1}
    10 {1 4, 2 3, 3 3, 4 3, 5 2}
    11 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1}
    12 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1}
    13 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1}
    14 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1}
    15 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1, 8 1}
    16 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1, 8 1}
    17 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1, 8 1, 9 1}
    18 {1 4, 2 3, 3 3, 4 3, 5 3, 6 1, 7 1, 8 1, 9 1}
    19 {1 4, 2 3, 3 3, 4 3, 5 3, 6 2, 7 1, 8 1, 9 1}
    20 {1 4, 2 3, 3 3, 4 3, 5 3, 6 2, 7 2, 8 1, 9 1}}

   :standard/half
   {2 {1 2}
    3 {1 3}
    4 {1 3}
    5 {1 4, 2 2}
    6 {1 4, 2 2}
    7 {1 4, 2 3}
    8 {1 4, 2 3}
    9 {1 4, 2 3, 3 2}
    10 {1 4, 2 3, 3 2}
    11 {1 4, 2 3, 3 3}
    12 {1 4, 2 3, 3 3}
    13 {1 4, 2 3, 3 3, 4 1}
    14 {1 4, 2 3, 3 3, 4 1}
    15 {1 4, 2 3, 3 3, 4 2}
    16 {1 4, 2 3, 3 3, 4 2}
    17 {1 4, 2 3, 3 3, 4 3, 5 1}
    18 {1 4, 2 3, 3 3, 4 3, 5 1}
    19 {1 4, 2 3, 3 3, 4 3, 5 2}
    20 {1 4, 2 3, 3 3, 4 3, 5 2}}

   :multiclass
   {1 {1 2}
    2 {1 3}
    3 {1 4, 2 2}
    4 {1 4, 2 3}
    5 {1 4, 2 3, 3 2}
    6 {1 4, 2 3, 3 3}
    7 {1 4, 2 3, 3 3, 4 1}
    8 {1 4, 2 3, 3 3, 4 2}
    9 {1 4, 2 3, 3 3, 4 3, 5 1}
    10 {1 4, 2 3, 3 3, 4 3, 5 2}
    11 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1}
    12 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1}
    13 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1}
    14 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1}
    15 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1, 8 1}
    16 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1, 8 1}
    17 {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1, 8 1, 9 1}
    18 {1 4, 2 3, 3 3, 4 3, 5 3, 6 1, 7 1, 8 1, 9 1}
    19 {1 4, 2 3, 3 3, 4 3, 5 3, 6 2, 7 1, 8 1, 9 1}
    20 {1 4, 2 3, 3 3, 4 3, 5 3, 6 2, 7 2, 8 1, 9 1}}})

(def std-slots-label "Spell Slots")


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

(defn filter-by-str
  "Filter's by :name using the given str"
  [filter-str coll]
  (if-not (str/blank? filter-str)
    (->> coll
         (filter (fn [{n :name}]
                   (wstr/includes-any-case? n filter-str))))
    coll))

(defn feature-by-id
  ([container feature-id]
   (or (get-in container [:features feature-id])
       (get-in container [:list-entities feature-id])))
  ([data-source container feature-id]
   (or (feature-by-id container feature-id)
       (feature-by-id data-source feature-id)
       (src-util/inflate-feature data-source container feature-id))))

(defn feature-in-lists [engine-state entity-lists id]
  (or (feature-by-id engine-state id)
      (some (fn [source]
              (get-in source [:features id]))
            (flatten entity-lists))))

(defn options-of-list
  [engine-state list-id options-set]
  (->> (engine/inflate-list engine-state list-id)
       (filter (comp (->set options-set) :id))))

; ======= 5e-specific nav =================================

(reg-sub :5e/page :5e/page)
(reg-sub :5e/actions-page :5e/actions-page)

(defn- page-specific-sheet
  [[sheet->page sheet-id] [_ default]]
  (get sheet->page sheet-id default))

(reg-sub
  ::page
  :<- [:5e/page]
  :<- [:active-sheet-id]
  :<- [::spellcaster-blocks]
  :<- [:device-type]
  (fn [[_sheet->page _sheet-id
        spell-classes device-type
        :as input] _]
    (let [smartphone? (= :smartphone device-type)
          default (if smartphone?
                    :abilities
                    :actions)
          base (page-specific-sheet input [nil default])]

      ; with keymaps, a user might accidentally go to :spells
      ; but not have spells; in that case, fall back to :actions
      (if-not (or (and (= :abilities base)
                       (not smartphone?))
                  (and (= :spells base)
                       (not (seq spell-classes))))
        ; normal case
        base

        ; fallback
        default))))

(reg-sub
  ::actions-page
  :<- [:5e/actions-page]
  :<- [:active-sheet-id]
  page-specific-sheet)


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
  :<- [::ability-modifiers]
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
  :<- [::ability-modifiers]
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
  ::abilities-raw
  :<- [:meta/sheet]
  (fn [sheet]
    (:abilities sheet)))

; NOTE: we compute these buffs by hand because we (potentially) need the
; dependent sub to compute other buffs
(reg-id-sub
  ::abilities-improvements
  :<- [:classes]
  :<- [:races]
  :<- [:effects]
  (fn [entity-lists]
    (->> entity-lists
         flatten
         (map (comp :buffs :attrs))
         (apply merge-with merge)
         (#(select-keys % (map first data/labeled-abilities)))
         (reduce-kv (fn [m abi buffs]
                      (assoc m abi (apply + (vals buffs))))
                    {})
         )))

(reg-id-sub
  ::abilities-racial
  :<- [:race]
  (fn [race]
    (-> race :attrs :5e/ability-score-increase)))

; ability scores are a function of the raw, rolled stats
; in the sheet, racial modififiers, and any ability score improvements
; from the class.
; TODO There are also equippable items, but we don't yet support that.
; TODO when we do handle equippable item buffs here, we need
; to make sure ::available-classes doesn't use it (only ability
; score improvements and racial bonuses ...)
(reg-id-sub
  ::abilities-base
  :<- [::abilities-raw]
  :<- [::abilities-racial]
  :<- [::abilities-improvements]
  (fn [[abilities race improvements]]
    (merge-with +
                abilities
                race
                improvements)))

(reg-id-sub
  ::abilities
  :<- [::abilities-base]
  :<- [:meta/sheet]
  (fn [[base sheet]]
    (merge-with +
                base
                (:ability-tmp sheet))))

(reg-id-sub
  ::ability-modifiers
  :<- [::abilities]
  (fn [abilities]
    (reduce-kv
     (fn [m ability score]
       (assoc m ability (ability->mod score)))
     {}
     abilities)))

(reg-id-sub
  ::ability-saves
  :<- [::ability-modifiers]
  :<- [::proficiency-bonus]
  :<- [::save-proficiencies]
  :<- [::buffs :saves]
  (fn [[modifiers prof-bonus save-proficiencies save-buffs]]
    (reduce-kv
      (fn [m ability modifier]
        (let [proficient? (get save-proficiencies ability)]
          (assoc m ability
                 (if proficient?
                   (mod->str
                     (+ modifier save-buffs prof-bonus))

                   (mod->str
                     (+ modifier save-buffs))))))
      {}
      modifiers)))

(reg-id-sub
  ::ability-info
  :<- [::abilities]
  :<- [::abilities-base]
  :<- [::ability-modifiers]
  :<- [::save-proficiencies]
  :<- [::ability-saves]
  (fn [[abilities base modifiers save-proficiencies saves]]
    (reduce-kv
      (fn [m ability score]
        (assoc m ability
               {:score score
                :modifier (mod->str (get modifiers ability))
                :save (get saves ability)
                :mod (let [delta (- score
                                    (get base ability))]
                       (cond
                         (= delta 0) nil
                         (> delta 0) :buff
                         :else :nerf))
                :proficient? (get save-proficiencies ability)}))
      {}
      abilities)))

(reg-id-sub
  ::skill-info
  :<- [::ability-modifiers]
  :<- [::skill-expertise]
  :<- [::skill-proficiencies]
  :<- [::skill-half-proficiencies]
  :<- [::proficiency-bonus]
  (fn [[modifiers expertise proficiencies half-proficiencies prof-bonus]]
    (reduce-kv
      (fn [m skill ability]
        (let [expert? (contains? expertise skill)
              half? (contains? half-proficiencies skill)
              proficient? (contains? proficiencies skill)]
          (assoc m skill
                 {:id skill
                  :ability ability
                  :expert? expert?
                  :half? half?
                  :proficient? proficient?
                  :modifier (+ (get modifiers ability)

                               ; NOTE: half proficiency is lower priority than
                               ; other proficiencies; you could have both, but
                               ; you don't want to use half if you're an expert!
                               (cond
                                 expert? (* 2 prof-bonus)
                                 proficient? prof-bonus
                                 half? (Math/floor
                                         (/ prof-bonus 2))))})))
      {}
      data/skill-id->ability)))

(reg-id-sub
  ::limited-use-configs
  :<- [:all-limited-use-configs]
  :<- [:total-level]
  :<- [::ability-modifiers]
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
      (subscribe [::usable-slot-for entity])))
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
     [::abilities]
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

; returns a set of ability ids
(reg-sub
  ::save-proficiencies
  :<- [:classes]
  (fn [classes _]
    (->> classes
         (filter :primary?)
         (mapcat :attrs)
         (filter (fn [[k v]]
                   (when (= v true)
                     (= "save-proficiency" (namespace k)))))
         (map (comp keyword name first))
         (into #{}))))

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

; returns a collection of feature ids
(reg-sub
  ::all-proficiencies
  :<- [:races]
  :<- [:classes]
  (fn [entity-lists _]
    (->> entity-lists
         flatten
         (mapcat :attrs)
         (keep (fn [[k v]]
                 (when (and v
                            (= "proficiency" (namespace k)))
                   k)))
         (into #{}))))

; returns a set of skill ids
(reg-sub
  ::skill-proficiencies
  :<- [::all-proficiencies]
  (fn [feature-ids _]
    (->> feature-ids
         (filter data/skill-feature-ids)
         (map (comp keyword name))
         (into #{}))))

; returns a set of skill ids
(reg-sub
  ::skill-half-proficiencies
  :<- [:classes]
  (fn [classes _]
    (->> classes
         (mapcat (comp keys :half-proficient :attrs))
         (into #{}))))


; returns a set of skill ids
(reg-sub
  ::skill-expertise
  :<- [:classes]
  (fn [classes _]
    (->> classes
         (mapcat :attrs)
         (filter (fn [[k v]]
                   (when (= v true)
                     (= "expertise" (namespace k)))))
         (map (comp keyword name first))
         (into #{}))))

(defn level->proficiency-bonus
  [level]
  (condp <= level
    17 6
    13 5
    9 4
    5 3
    ; else
    2))

(reg-sub
  ::proficiency-bonus
  :<- [:total-level]
  (fn [total-level _]
    (level->proficiency-bonus total-level)))

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
  :<- [::ability-modifiers]
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


; ======= combat ===========================================

(reg-sub
  ::armor-equipped?
  :<- [:equipped-sorted]
  (fn [equipped]
    (some data/armor? equipped)))

(reg-sub
  ::shield-equipped?
  :<- [:equipped-sorted]
  (fn [equipped]
    (some data/shield? equipped)))

(reg-sub
  ::ac
  :<- [:classes]
  :<- [:effects]
  :<- [::attuned-eq]
  :<- [::ability-modifiers]
  :<- [::buffs :ac]
  :<- [::armor-equipped?]
  :<- [::shield-equipped?]
  (fn [[classes effects equipped modifiers ac-buff armor? shield?]]
    (let [ac-sources (->> (concat classes
                                  effects
                                  equipped)
                          (mapcat (comp vals :5e/ac :attrs)))
          fn-context {:modifiers modifiers
                      :armor? armor?
                      :shield? shield?}]
      (+ ac-buff

         (apply max

                ; unarmored AC (not available if we have armor equipped)
                (+ 10
                   (:dex modifiers))

                (map #(% fn-context) ac-sources))))))

(reg-sub
  ::initiative
  :<- [::ability-modifiers]
  :<- [::skill-half-proficiencies]
  :<- [::proficiency-bonus]
  :<- [::buffs :initiative]
  (fn [[abilities half-proficiencies prof-bonus buffs]]
    (+ (:dex abilities)

       (when (contains? half-proficiencies :initiative)
         (Math/floor (/ prof-bonus 2)))

       buffs)))

(reg-sub
  ::attacks-per-action
  :<- [:classes]
  (fn [classes _]
    (or (->> classes
             (map (juxt (comp vals :attacks-per-action :attrs)
                        identity))
             (mapcat (fn [[values entity]]
                       (map
                         (fn [v]
                           (if (number? v)
                             ; easy case
                             v

                             ; functional value
                             (v entity)))
                         values)))
             (apply max))
        1)))

(reg-sub
  ::combat-info
  :<- [::attacks-per-action]
  :<- [:classes]
  (fn [[attacks-per-action classes] _]
    (->> classes
         (map (juxt (comp vals :combat-info :attrs)
                    identity))
         (mapcat (fn [[values entity]]
                   (map
                     (fn [v]
                       (update v :value
                               (fn [value]
                                 (value entity))))
                     values)))
         (sort-by :name)

         ; insert attacks
         (cons {:name "Attacks per action"
                :value attacks-per-action}))))

(reg-sub
  ::unarmed-strike
  :<- [:classes]
  :<- [:sheet-engine-state]
  :<- [::ability-modifiers]
  :<- [::proficiency-bonus]
  (fn [[classes data-source modifiers proficiency-bonus]]
    ; prefer the first non-implicit result
    (->> classes
         (map (fn [c]
                (assoc
                  (or (-> c :features :unarmed-strike)
                      ; possibly a custom class without a custom :unarmed-strike;
                      ; fall back gracefully to the default
                      (get-in data-source [:features :unarmed-strike]))
                  :wish/context c
                  :wish/context-type :class)))

         ; sort-by is in ascending order
         (sort-by #(if (:implicit? %)
                     1
                     0))

         ; insert :to-hit calculation
         (map (fn [s]
                (assoc s
                       :to-hit (if (:finesse? s)
                                 (+ proficiency-bonus
                                    (max (:str modifiers)
                                         (:dex modifiers)))
                                 (+ proficiency-bonus (:str modifiers)))
                       :dmg (invoke-callable
                               s :dice
                               :modifiers modifiers))))

         first)))

(reg-sub
  ::ammunition-for
  :<- [::inventory-sorted]
  (fn [items [_ _weapon]]
    (->> items
         (filter (comp (partial = :ammunition) :type))
         seq)))

; ======= items and equipment ==============================

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

; all equipped items that are attuned (or that don't need to be attuned)
(reg-sub
  ::attuned-eq
  :<- [:equipped-sorted]
  :<- [::attuned-ids]
  (fn [[equipped attuned-set]]
    (remove
      (fn [item]
        (and (:attunes? item)
             (not (contains? attuned-set (:id item)))))
      equipped)))

(reg-sheet-sub
  ::attuned-ids
  :attuned)

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

; returns a set of weapon kind ids that should always be treated
; as "finesse" weapons
(reg-sub
  ::finesse-weapon-kinds
  :<- [:classes]
  (fn [classes]
    (->> classes
         ; combine all class-specific lists
         (mapcat (comp :finesse-weapon-kinds :lists))

         (map (fn [{:keys [id]}]
                (if (namespace id)
                  ; unpack eg :proficiency/longsword
                  (keyword (name id))

                  ; regular weapon kind id
                  id)))
         set)))

(defn- compute-bonus [effects modifiers buff]
  (when (fn? buff)
    (buff {:effects effects :modifiers modifiers})))

(defn calculate-weapon
  [proficient-cats proficient-kinds
   effects-set modifiers
   ; NOTE we have to provide a type hint for the compiler
   ; here for some reason....
   ^number proficiency-bonus,
   atk-bonuses dmg-bonuses
   finesse-weapon-kinds
   w]
  (let [{weap-bonus :+
         :keys [kind category ranged? finesse?]} w

        ; we can be proficient in either the weapon's specific kind
        ; (eg :longbow) or its category (eg :martial)
        proficient? (or (proficient-kinds kind)
                        (proficient-cats category))

        ; some classes can treat certain weapon kinds as
        ; finesse even if they aren't naturally (eg: monk)
        finesse? (or finesse?
                     (contains? finesse-weapon-kinds
                                kind))

        ; we can use dex bonus if it's a ranged weapon OR if it's
        ; finesse?
        dex-bonus (when (or ranged? finesse?)
                    (:dex modifiers))

        ; we can use str bonus only for melee weapons
        str-bonus (when (not ranged?)
                    (:str modifiers))

        ; this is also added to the atk roll
        chosen-bonus (max dex-bonus str-bonus)

        prof-bonus (when proficient?
                     proficiency-bonus)

        weap-type-key (if ranged?
                        :ranged
                        :melee)

        dmg-bonus-maps (concat (->> dmg-bonuses weap-type-key vals)
                               (->> dmg-bonuses :any vals))
        atk-bonuses (->> atk-bonuses weap-type-key vals)

        ; raw bonus maps {:+,:when-versatile?}
        other-bonus (->> dmg-bonus-maps
                         (filter #(= (:when-two-handed? %)
                                     (boolean (:two-handed? w)))))

        other-bonus-any (->> other-bonus
                             (filter #(:when-versatile? % true))
                             (keep :+))
        other-bonus-versatile (->> other-bonus
                                   (filter :when-versatile?)
                                   (keep :+))
        other-bonus-non-versatile (->> other-bonus
                                       (remove :when-versatile?)
                                       (keep :+))

        computed-dmg-bonuses (->> dmg-bonus-maps
                                  (keep (partial compute-bonus effects-set modifiers))
                                  (apply +))

        ; TODO indicate dmg type?
        other-dice-bonuses (keep :dice dmg-bonus-maps)

        stat-bonus (let [b (+ weap-bonus chosen-bonus)]
                     (when (not= b 0)
                       b))

        dam-bonus (when stat-bonus
                    stat-bonus
                    (apply + stat-bonus other-bonus-any))

        ; fn that accepts a coll of constant bonus values
        ; and generates a string of "+ <bonus>" including
        ; all other dice bonuses, etc.
        dam-bonuses (fn [const-bonuses]
                      (let [base (->> (cons
                                        (apply + dam-bonus computed-dmg-bonuses const-bonuses)
                                        other-dice-bonuses)
                                      (keep identity)
                                      (str/join " + "))]
                        (when-not (str/blank? base)
                          (str " + " base))))]

    ; NOTE I don't *think* weapon damage ever scales?
    (assoc w
           :base-dice (str (:dice w) (dam-bonuses other-bonus-non-versatile))
           :alt-dice (when-let [versatile (:versatile w)]
                       (str versatile (dam-bonuses other-bonus-versatile)))
           :to-hit (apply + stat-bonus prof-bonus atk-bonuses))))

(reg-sub
  ::equipped-weapons
  :<- [:equipped-sorted]
  :<- [::eq-proficiencies]
  :<- [::proficiency-bonus]
  :<- [:effect-ids-set]
  :<- [::ability-modifiers]
  :<- [::buff-attrs :atk]
  :<- [::buff-attrs :dmg]
  :<- [::finesse-weapon-kinds]
  (fn [[all-equipped
        proficiencies proficiency-bonus
        effects-set modifiers
        atk-bonuses dmg-bonuses
        finesse-weapon-kinds]]
    (let [{proficient-kinds :kinds
           proficient-cats :categories} proficiencies]
      (->> all-equipped
           (filter #(= :weapon (:type %)))
           (map
             (partial calculate-weapon
                      proficient-cats proficient-kinds
                      effects-set modifiers
                      proficiency-bonus
                      atk-bonuses dmg-bonuses
                      finesse-weapon-kinds))))))

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


; ======= combat ==========================================

(reg-sub
  ::actions-for-type
  :<- [:sheet-engine-state]
  :<- [:classes]
  :<- [:races]
  :<- [::attuned-eq]
  :<- [:effects]
  (fn [[data-source & entity-lists] [_ filter-type]]
    (->> entity-lists
         flatten
         (mapcat
           (fn [c]
             (map (fn [[id flags]]
                    (with-meta
                      (let [action (or (when (= id (:id c))
                                         ; attuned equipment, probably
                                         c)
                                       (feature-by-id data-source c id))]

                        (-> action
                            (update :desc (fn [d]
                                            (if (fn? d)
                                              (d c)
                                              d)))))

                      (cond
                        (map? flags) flags
                        (keyword? flags) {flags true}
                        :else nil)))
                  (get-in c [:attrs filter-type]))))
         (keep identity)
         (sort-by :name))))

(reg-sub
  ::special-combat-actions
  :<- [::actions-for-type :special-action]
  (fn [actions _]
    (filter
      #(:combat (meta %))
      actions)))

(reg-sub
  ::other-attacks
  :<- [:sheet-engine-state]
  :<- [:meta/options]
  :<- [::ability-modifiers]
  :<- [::proficiency-bonus]
  :<- [:total-level]
  :<- [:races]
  :<- [:classes]
  :<- [:effects]
  (fn [[data-source options modifiers
        prof-bonus total-level
        & entity-lists] _]
    (->> entity-lists
         flatten
         (keep (juxt (comp :attacks :attrs)
                     identity))

         ; transform from seq of maps of attack-maps into
         ; a seq of attack-maps, including the :wish/source
         ; each came from
         (mapcat (fn [[attacks-map source]]
                   (when-let [attacks (seq attacks-map)]
                     ; NOTE: attacks is a sequence of [id attack-map] pairs
                     (map (fn [[id attack]]
                            (assoc attack
                                   :id id
                                   ; provide :total-level if it doesn't have :level
                                   :wish/source
                                   (update source :level #(or % total-level))))
                          attacks))))

         ; fill out each map, inflating :dice values, etc. and
         ; merging in &from-options as necessary
         (map (fn [attack]
                (let [source-entity (:wish/source attack)
                      context-feature (feature-by-id data-source source-entity (:id attack))
                      attack (if-let [from-option (:&from-option attack)]
                               ; merge in
                               (let [opt (->> options
                                              from-option
                                              first
                                              (src-util/inflate-feature
                                                data-source source-entity))]
                                 (merge
                                   opt

                                   ; use the context-feature's desc, if possible
                                   (when context-feature
                                     (select-keys
                                       context-feature
                                       [:desc]))

                                   attack))

                               ; nothing to do
                               attack)

                      info (assoc source-entity
                                  :modifiers modifiers
                                  :prof-bonus prof-bonus)
                      dice-fn (:dice attack)
                      save-fn (:save-dc attack)]
                  (-> attack
                      (assoc :dmg (dice-fn info))
                      (assoc :save-dc (save-fn info)))))))))


; ======= Spells ===========================================

(reg-sub
  :5e/spells-filter
  (fn [db]
    (:5e/spells-filter db nil)))


(defn knowable-spell-counts-for
  [spellcaster modifiers]
  (let [{:keys [id level cantrips slots known]} spellcaster

        spells (cond
                 known (get known (dec level))

                 ; NOTE: :standard is the default if omitted
                 (or (nil? slots)
                     (= :standard slots)) (max
                                            1
                                            (+ level
                                               (get modifiers id)))

                 (= :standard/half slots) (max
                                            1
                                            (+ (js/Math.ceil (/ level 2))
                                               (get modifiers id))))

        cantrips (->> cantrips
                      (partition 2)
                      (reduce
                        (fn [cantrips-known [at-level incr]]
                          (if (>= level at-level)
                            (+ cantrips-known incr)
                            (reduced cantrips-known)))
                        0))]

    {:spells spells
     :cantrips cantrips}))

; returns eg: {:cleric {:spells 4, :cantrips 2}}
(reg-sub
  ::knowable-spell-counts-by-class
  :<- [::spellcaster-blocks]
  :<- [::spellcasting-modifiers]
  (fn [[spellcasters modifiers]]
    (reduce
      (fn [m s]
        (assoc m (:id s) (knowable-spell-counts-for s modifiers)))
      {}
      spellcasters)))

(reg-sub
  ::knowable-spell-counts
  :<- [::knowable-spell-counts-by-class]
  (fn [counts [_ class-id]]
    (get counts class-id)))

; NOTE: this function is an absolute beast, and could benefit from
; some unit testing for sure, and probably some refactoring. It's
; responsible for handling all the possible caster types, different
; ways of acquiring spells (IE: in spellbook, provided by class
; features, etc)
(defn inflate-prepared-spells-for-caster
  [total-level engine-state modifiers
   attack-bonuses spell-buffs options
   caster-attrs]
  (let [attrs caster-attrs
        caster-id (:id attrs)
        c (:wish/container attrs)
        {:keys [acquires? prepares?]} attrs
        spells-list (:spells attrs)
        extra-spells-list (:extra-spells attrs)

        ; NOTE: we namespace spell mods by the class/race id in case
        ; we ever want to combine all :attrs of a character into
        ; a single map.
        spell-mods (get-in c [:attrs :spells caster-id])

        ; if we acquire AND prepare spells, the source list is still
        ; the same, but we use the :acquires?-spells option-list to
        ; determine which are actually prepared (the normal :spells
        ; list just indicates which spells are *acquired*)
        spells-option (cond
                        ; if an explicit list was provided, use it
                        (:prepared-spells attrs)
                        (:prepared-spells attrs)

                        (and acquires? prepares?)
                        (:acquires?-spells attrs)

                        ; the normal list
                        :else spells-list)

        ; FIXME it should actually be the intersection of acquired
        ; and prepared, in case they un-learn a spell but forget
        ; to un-prepare it. This is an edge case, but we should
        ; be graceful about it. Alternatively, unlearning a spell
        ; should also eagerly un-prepare it.

        ; all spells from the extra-spells list
        ; NOTE: because extra spells are provided
        ; by features and levels, we can't find them
        ; in the data source.
        ; ... unless it's a collection of spell ids
        extra-spells (when extra-spells-list
                       (engine/inflate-list
                         engine-state c options extra-spells-list))

        ; extra spells are always prepared
        extra-spells (some->> extra-spells
                              (map #(assoc % :always-prepared? true)))

        selected-spell-ids (get options spells-option [])

        ; only selected spells from the main list (including those
        ; added by class features, eg warlock)
        selected-spells (engine/inflate-list
                          engine-state c options selected-spell-ids)

        ; for :acquires? spellcasters, their acquired
        ; cantrips are always prepared
        selected-spells (if acquires?
                          (->> (engine/inflate-list
                                 engine-state c options (get options spells-list []))
                               (filter #(= 0 (:spell-level %)))
                               (map #(assoc % :always-prepared? true))

                               ; plus manually prepared spells
                               (concat selected-spells))

                          selected-spells)]

    (->> (concat
           extra-spells
           selected-spells)

         (map #(-> %
                   (assoc
                     ::source caster-id
                     :total-level total-level
                     :spell-mod (get modifiers caster-id)
                     :save-label (when-let [k (:save %)]
                                   (str/upper-case
                                     (name k)))

                     :buffs (if (:damage %)
                              (:dmg spell-buffs)
                              (:healing spell-buffs))

                     ; save dc is attack modifier + 8
                     :save-dc (+ (get attack-bonuses caster-id)
                                 8))
                   (merge (get spell-mods (:id %)))))

         ; sort by level, then name
         (sort-by (juxt :spell-level :name)))))

(reg-sub
  ::prepared-spells-by-class
  :<- [:sheet-engine-state]
  :<- [::spellcaster-blocks]
  :<- [:total-level]
  :<- [::spellcasting-modifiers]
  :<- [::spell-attack-bonuses]
  :<- [::spell-buffs]
  :<- [:meta/options]
  (fn [[engine-state spellcasters total-level modifiers
        attack-bonuses spell-buffs options]]
    (some->> spellcasters
             seq
             (reduce
               (fn [m {caster-id :id :as attrs}]
                 (assoc m caster-id
                        (inflate-prepared-spells-for-caster
                          total-level engine-state modifiers
                          attack-bonuses spell-buffs options
                          attrs)))
               {}))))

; count of explicitly acquired (IE not added by class features, etc.)
; spells in the given spells-list
(reg-sub
  ::acquired-spells-count
  :<- [:sheet-engine-state]
  :<- [:meta/options]
  (fn [[data-source options] [_ spells-list]]
    (let [selected-ids (get options spells-list)]
      (->> (engine/inflate-list data-source selected-ids)
           (filter #(not= 0 (:spell-level %)))
           count))))

; just (get [::prepared-spells-by-class] class-id)
(reg-sub
  ::prepared-spells
  :<- [::prepared-spells-by-class]
  (fn [by-class [_ class-id]]
    (get by-class class-id)))

(reg-sub
  ::prepared-spells-filtered
  :<- [::all-prepared-spells]
  (fn [spells [_ filter-type]]
    (when-not (= :special-action filter-type)
      (filter (case filter-type
                :action (fn [s]
                          (and (not (util/bonus-action? s))
                               (not (util/reaction? s))))
                :bonus util/bonus-action?
                :reaction util/reaction?

                (if (number? filter-type)
                  #(= filter-type (:level %))

                  (throw (js/Error.
                           (str "Unknown spell filter-type:" filter-type)))))
              spells))))

; reduces ::prepared-spells into {:cantrips, :spells},
; AND removes ones that are always prepared
(reg-sub
  ::my-prepared-spells-by-type

  (fn [[_ class-id]]
    (subscribe [::prepared-spells class-id]))

  (fn [spells]
    (->> spells
         (remove :always-prepared?)
         (reduce
           (fn [m s]
             (let [spell-type (if (= 0 (:spell-level s))
                                :cantrips
                                :spells)]
               (update m spell-type conj s)))
           {:cantrips []
            :spells []}))))

; list of all spells on a given spell list for the given spellcaster block,
; with `:prepared? bool` inserted as appropriate
(reg-sub
  ::preparable-spell-list

  (fn [[_ spellcaster _list-id]]
    [(subscribe [:sheet-engine-state])
     (subscribe [:meta/options])
     (subscribe [::prepared-spells (:id spellcaster)])
     (subscribe [::highest-spell-level-for-spellcaster-id (:id spellcaster)])
     (subscribe [:5e/spells-filter])])

  (fn [[engine-state options prepared-spells highest-spell-level
        filter-str]
       [_ spellcaster list-id]]
    (let [; is this the prepared list for an acquires? spellcaster?
          acquires-list? (= (:acquires?-spells spellcaster)
                            list-id)

          ; are we listing spells that an acquires? spellcaster *can* acquire?
          acquire-mode? (and (:acquires? spellcaster)
                             (not acquires-list?))

          prepared-set (if acquire-mode?
                         ; in acquire mode, the "prepared set" is actually
                         ; the "acquired set"
                         (get options (:spells spellcaster) #{})

                         (->> prepared-spells
                              (map :id)
                              set))

          ; for an acquires? spellcaster, their cantrips are "always prepared,"
          ; but we should be able to un-acquire them in case of mis-clicks, etc.
          always-prepared-set (->> (if acquire-mode?
                                     (->> prepared-spells
                                          (remove #(= 0 (:spell-level %))))

                                     ; not acquire-mode; all :always-prepared?
                                     ; go into the set
                                     prepared-spells)
                                   (filter :always-prepared?)
                                   (map :id)
                                   set)

          source (if acquires-list?
                   ; if we want to look at the :acquired? list, its
                   ; source is actually the selected from (:spells)
                   ; NOTE: do we need to concat class-provided lists?
                   (->> (options-of-list
                          engine-state (:spells spellcaster)
                          (get options (:spells spellcaster) #{})))

                   ; normal case:
                   (engine/inflate-list
                     engine-state (:wish/container spellcaster)
                     options
                     list-id))

          spells-filter (if-let [filter-fn (:values-filter spellcaster)]
                          ; let the spellcaster determine the filter
                          (fn [spell]
                            (filter-fn (assoc spell :level (:level spellcaster))))

                          ; limit visible spells by those actually available
                          ; (IE it must be of a level we can prepare)
                          #(<= (:spell-level %) highest-spell-level))]

      (->> source
           (filter spells-filter)

           (filter-by-str filter-str)

           ; sort by level then name
           (sort-by (juxt :spell-level :name))

           (map #(if (always-prepared-set (:id %))
                   (assoc % :always-prepared? true)
                   %))
           (map #(if (prepared-set (:id %))
                   (assoc % :prepared? true)
                   %))))))

; list of all prepared spells across all classes
(reg-sub
  ::all-prepared-spells
  :<- [::prepared-spells-by-class]
  (fn [spells-by-class]
    (->> spells-by-class
         vals
         flatten)))

(reg-sub
  ::spell-attacks
  :<- [::spell-attack-bonuses]
  :<- [::all-prepared-spells]
  (fn [[bonuses prepared-spells]]
    (->> prepared-spells
         (filter :attack)
         (map (fn [s]
                (assoc s
                       :to-hit (get bonuses
                                    (::source s))

                       :base-dice (invoke-callable
                                    s
                                    :dice)))))))

(reg-sub
  ::spellcaster-blocks
  :<- [:classes]
  :<- [:races]
  (fn [spellcaster-collections]
    (->> spellcaster-collections
         flatten
         (mapcat (fn [c]
                   (when-let [sc-map (-> c :attrs :5e/spellcaster)]
                     (map (fn [[id spellcaster]]
                            (-> spellcaster
                                (assoc :id id
                                       :wish/container c
                                       :level (:level c))
                                (cond->
                                  (not (:name spellcaster))
                                  (assoc :name (:name c)))))
                          sc-map)))))))

(reg-sub
  ::spellcaster-blocks-by-id
  :<- [::spellcaster-blocks]
  (fn [blocks]
    (->map blocks)))

(reg-sub
  ::spellcaster-block-by-id
  :<- [::spellcaster-blocks-by-id]
  (fn [blocks [_ spellcaster-id]]
    (get blocks spellcaster-id)))

(reg-sub
  ::spellcaster-info
  :<- [::spellcasting-modifiers]
  :<- [::spell-attack-bonuses]
  (fn [[modifiers atk-bonuses] [_ spellcaster-id]]
    (let [atk (get atk-bonuses spellcaster-id)]
      {:mod (get modifiers spellcaster-id)
       :attack atk
       :save-dc (+ 8 atk)})))

(reg-sub
  ::spellcasting-modifiers
  :<- [::abilities]
  :<- [::spellcaster-blocks]
  (fn [[abilities spellcasters]]
    (reduce
      (fn [m c]
        (let [spellcasting-ability (-> c :ability)]
          (assoc m
                 (:id c)
                 (ability->mod
                   (get abilities spellcasting-ability)))))
      {}
      spellcasters)))

(reg-sub
  ::eq-attack-buffs
  :<- [::attuned-eq]
  (fn [eq _]
    (->> eq
         (filter (fn [{id :id}]
                   ; hacks?
                   (some (partial str/starts-with? (name id))
                         ["rod-" "staff-" "wand-"])))
         (map :+)
         (apply +))))

(reg-sub
  ::spell-attack-bonuses
  :<- [::spellcasting-modifiers]
  :<- [::proficiency-bonus]
  :<- [::eq-attack-buffs]
  (fn [[modifiers proficiency-bonus buffs]]
    (reduce-kv
      (fn [m class-or-race-id modifier]
        (assoc m class-or-race-id (+ proficiency-bonus modifier buffs)))
      {}
      modifiers)))

(defn comp-buffs
  "Returns a composed function that computes the buffs
   for a given spell"
  [all-buffs]
  (let [consts (->> all-buffs
                    (filter number?)
                    (apply +))
        fns (->> all-buffs
                 (filter fn?))]
    (fn compute-buffs [s]
      (let [buffs (+ consts
                     (reduce
                       (fn [total f]
                         (+ total (f s)))
                       0
                       fns))]
        (when-not (= 0 buffs)
          buffs)))))

(reg-sub
  ::spell-buffs
  :<- [:all-attrs]
  (fn [attrs]
    (->> attrs
         :buffs
         :spells
         (reduce-kv
           (fn [m kind buffs-map]
             (assoc m kind
                    (->> buffs-map
                         vals
                         comp-buffs)))
           {}))))

(defn- standard-spell-slots? [c]
  (= :standard (:slots-type c :standard)))

(defn spell-slots
  [spellcasters]
  (if (= 1 (count spellcasters))
    (let [spellcaster (first spellcasters)
          level (:level spellcaster)
          kind (:slots-type spellcaster :standard)
          label (:slots-label spellcaster std-slots-label)
          schedule (:slots spellcaster :standard)
          schedule (or (when (keyword? schedule)
                         (schedule spell-slot-schedules))
                       schedule)]
      (if-not (= :none schedule)
        {kind {:label label
               :slots (get schedule level)}}
        (select-keys spellcaster [:cantrips])))

    (let [std-level (apply
                      +
                      (->> spellcasters
                           (filter standard-spell-slots?)
                           (map
                             (fn [c]
                               (let [mod (:multiclass-levels-mod c 1)]
                                 (when-not (= mod 0)
                                   (int
                                     (Math/floor
                                       (/ (:level c)
                                          mod)))))))))]
      (apply
        merge
        {:standard
         {:label std-slots-label
          :slots (get-in spell-slot-schedules [:multiclass std-level])}}
        (->> spellcasters
             (filter (complement standard-spell-slots?))
             (map (fn [c]
                    (spell-slots [c]))))))))

(reg-sub
  ::spellcaster-blocks-with-slots
  :<- [::spellcaster-blocks]
  (fn [all-blocks]
    (filter #(not= :none (:slots %))
            all-blocks)))

(reg-sub
  ::spell-slots
  :<- [::spellcaster-blocks-with-slots]
  spell-slots)

(reg-sub
  ::spell-slots-for-spellcaster-id
  (fn [[_ spellcaster-id]]
    (subscribe [::spellcaster-block-by-id spellcaster-id]))
  (fn [spellcaster]
    (spell-slots [spellcaster])))

(defn highest-spell-level-for-slots [slots]
  (or (when (map? (:slots slots))
        (some->> slots
                 :slots

                 ; highest spell level available
                 keys
                 (apply max)))

      (when (> (some->> slots :cantrips second) 0)
        0)))

(reg-sub
  ::highest-spell-level-for-spellcaster-id
  (fn [[_ spellcaster-id]]
    (subscribe [::spell-slots-for-spellcaster-id spellcaster-id]))
  (fn [slots-by-type]
    (highest-spell-level-for-slots
      ; NOTE: we only have one slot type to look at since we're looking
      ; at a specific spellcaster type
      (some->> slots-by-type vals first))))

(reg-sub
  ::highest-spell-level
  :<- [::spell-slots]
  (fn [all-slots]
    (->> all-slots
         vals
         (mapcat (comp keys :slots))
         (apply max))))

(reg-sub
  ::spellcaster-slot-types
  :<- [::spellcaster-blocks-with-slots]
  (fn [classes]
    (->> classes
         (filter (complement standard-spell-slots?))
         (map #(name (:slots-type %)))
         set)))

(reg-sub
  ::spell-slots-used
  :<- [:limited-used]
  :<- [::spellcaster-slot-types]
  (fn [[used slot-types]]
    (reduce-kv
      (fn [m id used]
        (let [id-ns (namespace id)
              level (-> id name wstr/last-char int)]
          (cond
            (= "slots" id-ns)
            (assoc-in m [:standard level] used)

            (contains? slot-types id-ns)
            (assoc-in m [(keyword id-ns) level] used)

            :else m)))  ; ignore; unrelated
      {}
      used)))

(defn available-slots
  [slots used]
  (->> slots
       (map identity) ; convert to [kind, {info}]
       (mapcat
         (fn [[kind {levels :slots}]]
           (keep (fn [[level total]]
                   (let [unused (- total
                                   (get-in used [kind level]))]
                     (when (> unused 0)
                       {:kind kind
                        :level level
                        :total total
                        :unused unused})))
                 levels)))
       (sort-by :level)))

; returns [{:kind,:level,:total,:unused}]
; sorted in ascending order by :level
(reg-sub
  ::available-slots
  :<- [::spell-slots]
  :<- [::spell-slots-used]
  (fn [[slots used] _]
    (available-slots slots used)))

(defn usable-slots-for
  [slots spellcaster s]
  (let [{use-id :consumes
         :keys [spell-level at-will?]} s
        limited-use? (and use-id
                          (not= :*spell-slot use-id))
        cantrip? (= 0 spell-level)
        at-will? (or cantrip? at-will?)
        no-slots? (= :none (:slots spellcaster))]
    (when-not (or limited-use? at-will?
                  ; no-slot casters can still have a slot
                  ; if it's a spell-slot-based limited use
                  (when-not (= :*spell-slot use-id)
                    no-slots?))
      ; if it's at-will or powered by a limited-use,
      ; there's no possible usable slot because it doesn't
      ; use *any* slots.
      ; Of course, if the limited-use is the special
      ; :*spell-slot type, then we *can* use slots
      (->> slots
           (filter #(>= (:level %)
                        spell-level))))))

(reg-sub
  ::usable-slots-for
  (fn [[_ s]]
    [(subscribe [::available-slots])
     (subscribe [::spellcaster-block-by-id (::source s)])])
  (fn [[slots spellcaster] [_ s]]
    (usable-slots-for slots spellcaster s)))

; returns {:kind, :level} if any
(reg-sub
  ::usable-slot-for
  (fn [[_ s]]
    (subscribe [::usable-slots-for s]))
  (fn [usable-slots _]
    (first usable-slots)))


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
  :<- [::abilities-raw]
  (fn [abilities]
    (- point-buy-max
       (calculate-scores-cost abilities))))

(reg-sub
  ::point-buy-delta
  :<- [::abilities-raw]
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
  :<- [::abilities-base]
  (fn [[all-classes selected-classes primary-class abilities]]
    (available-classes
      all-classes selected-classes primary-class abilities)))

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
     (subscribe [::highest-spell-level-for-spellcaster-id entity-id])])
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
