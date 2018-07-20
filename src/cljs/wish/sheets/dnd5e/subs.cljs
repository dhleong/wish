(ns ^{:author "Daniel Leong"
      :doc "dnd5e.subs"}
  wish.sheets.dnd5e.subs
  (:require-macros [wish.util.log :as log :refer [log]])
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub subscribe]]
            [wish.sources.core :as src :refer [expand-list find-class find-race]]
            [wish.sheets.dnd5e.util :as util :refer [ability->mod ->die-use-kw]]
            [wish.util :refer [invoke-callable]]))

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

; ======= class and level ==================================

(reg-sub
  ::class->level
  :<- [:classes]
  (fn [classes _]
    (reduce
      (fn [m c]
        (assoc m (:id c) (:level c)))
      {}
      classes)))

(reg-sub
  ::class-level
  :<- [::class->level]
  (fn [classes [_ class-id]]
    (get classes class-id)))


; ability scores are a function of the raw, rolled stats
; in the sheet, racial modififiers, and any ability score improvements
; from the class.
; TODO There are also equippable items, but we don't yet support that.
(reg-sub
  ::abilities
  :<- [:sheet]
  :<- [:race]
  :<- [:classes]
  (fn [[sheet race classes]]
    (apply merge-with +
           (:abilities sheet)
           (-> race :attrs :5e/ability-score-increase)
           (map (comp :buffs :attrs) classes))))

(reg-sub
  ::ability-modifiers
  :<- [::abilities]
  (fn [abilities]
    (reduce-kv
     (fn [m ability score]
       (assoc m ability (ability->mod score)))
     {}
     abilities)))

(reg-sub
  ::limited-uses
  :<- [:limited-uses]
  :<- [:total-level]
  :<- [::ability-modifiers]
  :<- [::attuned-ids]
  (fn [[items total-level modifiers attuned-set]]
    (->> items
         (remove :implicit?)

         ; eagerly evaluate :uses (the sheet shouldn't do this)
         (map #(assoc % :uses
                      (invoke-callable % :uses
                                       :modifiers modifiers
                                       :total-level total-level)))

         ; remove uses that come from un-attuned items that require attunement
         (remove (fn [item]
                   (and (= :item (:wish/context-type item))
                        (:attunes? (:wish/context item))
                        (not (contains? attuned-set (:id (:wish/context item)))))))

         (sort-by :name))))

(reg-sub
  ::limited-use
  :<- [::limited-uses]
  :<- [:limited-used]
  (fn [[items used] [_ id]]
    (->> items
         (filter #(= id (:id %)))
         (map #(assoc % :uses-left (- (:uses %)
                                      (get used id))))
         first)))

(reg-sub
  ::rolled-hp
  :<- [:sheet]
  (fn [sheet [_ ?path]]
    (get-in sheet (concat
                    [:hp-rolled]
                    ?path))))

(reg-sub
  ::temp-hp
  :<- [:sheet]
  (fn [sheet _]
    (:temp-hp sheet)))

(reg-sub
  ::temp-max-hp
  :<- [:sheet]
  (fn [sheet _]
    (:temp-max-hp sheet)))

(reg-sub
  ::max-hp
  :<- [::rolled-hp]
  :<- [::temp-max-hp]
  :<- [::abilities]
  :<- [:total-level]
  :<- [::class->level]
  (fn [[rolled-hp temp-max abilities total-level class->level]]
    (apply +
           temp-max

           (* total-level
              (->> abilities
                   :con
                   ability->mod))

           ; if you set a class to level 3, set HP, then go back
           ; to level 2, there will be an orphaned entry in the
           ; rolled-hp vector. We could remove that entry when
           ; changing the level, but accounting for it here means
           ; that an accidental level-down doesn't lose your data
           (reduce-kv
             (fn [all-entries class-id class-rolled-hp]
               (concat all-entries
                       (take (class->level class-id)
                             class-rolled-hp)))
             nil
             rolled-hp))))

(reg-sub
  ::hp
  :<- [::temp-hp]
  :<- [::max-hp]
  :<- [:limited-used]
  (fn [[temp-hp max-hp limited-used-map]]
    (let [used-hp (or (:hp#uses limited-used-map)
                      0)]
      [(+ temp-hp
          (- max-hp used-hp)) max-hp])))

(reg-sub
  ::death-saving-throws
  :<- [:sheet]
  (fn [sheet]
    (:death-saving-throws sheet)))


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

; returns a const number
(reg-sub
  ::save-buffs
  :<- [:races]
  :<- [:classes]
  :<- [:equipped-sorted]
  (fn [entity-lists _]
    (->> entity-lists
         flatten
         (mapcat (comp vals :saves :buffs :attrs))
         (apply +))))


; returns a collection of features
(reg-sub
  ::save-extras
  :<- [:races]
  :<- [:classes]
  :<- [:equipped-sorted]
  (fn [entity-lists _]
    (->> entity-lists
         flatten
         (mapcat (comp :saves :attrs))
         (map (fn [[id extra]]
                (if (:id extra)
                  extra

                  ; shorthand:
                  (assoc extra :id id)))))))


; returns a set of skill ids
(reg-sub
  ::skill-proficiencies
  :<- [:races]
  :<- [:classes]
  (fn [entity-lists _]
    (->> entity-lists
         flatten
         (mapcat :attrs)
         (filter (fn [[k v]]
                   (when (= v true)
                     (= "proficiency" (namespace k)))))
         (map (comp keyword name first))
         (into #{}))))

; returns a set of skill ids
(reg-sub
  ::skill-expertise
  :<- [:classes]
  (fn [classes _]
    ; TODO expertise support
    #{}))

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
  ::speed
  :<- [:race]
  (fn [race]
    ; TODO other mods to speed?
    (-> race :attrs :5e/speed)))


; ======= combat ===========================================

(reg-sub
  ::ac
  :<- [:classes]
  :<- [:equipped-sorted]
  :<- [::ability-modifiers]
  (fn [[classes equipped modifiers]]
    (let [ac-sources (->> (concat classes
                                  equipped)
                          (mapcat (comp vals :5e/ac :attrs)))
          ac-buff (->> (concat classes
                               equipped)
                       (mapcat (comp vals :ac :buffs :attrs))
                       (apply +))
          fn-context {:modifiers modifiers}]
      (+ ac-buff

         (apply max

                ; unarmored AC (not available if we have armor equipped)
                (+ 10
                   (:dex modifiers))

                (map #(% fn-context) ac-sources)
                )))))

(reg-sub
  ::initiative
  :<- [::ability-modifiers]
  (fn [abilities]
    ; TODO other initiative mods?
    (:dex abilities)))

(reg-sub
  ::unarmed-strike
  :<- [:classes]
  :<- [::ability-modifiers]
  :<- [::proficiency-bonus]
  (fn [[classes modifiers proficiency-bonus]]
    ; prefer the first non-implicit result
    (->> classes
         (map (fn [c]
                (assoc
                  (-> c :features :unarmed-strike)
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


; ======= items and equipment ==============================

(reg-sub
  ::attuned-ids
  :<- [:sheet]
  (fn [sheet _]
    (get sheet :attuned)))

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

(reg-sub
  ::damage-bonuses
  :<- [:classes]
  (fn [classes]
    (->> classes
         (map (comp :dmg :buffs :attrs))
         (apply merge))))

; returns a set of weapon kind ids that should always be treated
; as "finesse" weapons
(reg-sub
  ::finesse-weapon-kinds
  :<- [:classes]
  :<- [:sheet-source]
  (fn [[classes source]]
    (->> classes
         ; combine all class-specific lists
         (mapcat (comp :finesse-weapon-kinds :lists))

         (map (fn [{:keys [id]}]
                (if-let [n (namespace id)]
                  ; unpack eg :proficiency/longsword
                  (keyword (name id))

                  ; regular weapon kind id
                  id)))
         set)))

(defn calculate-weapon
  [proficient-cats proficient-kinds
   modifiers
   ; NOTE we have to provide a type hint for the compiler
   ; here for some reason....
   ^number proficiency-bonus,
   dmg-bonuses
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

        bonus-maps (->> dmg-bonuses weap-type-key vals)

        ; raw bonus maps {:+,:when-versatile?}
        other-bonus (->> bonus-maps
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

        ; TODO indicate dmg type?
        other-dice-bonuses (keep :dice bonus-maps)

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
                                        (apply + dam-bonus const-bonuses)
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
           :to-hit (+ stat-bonus prof-bonus))))

(reg-sub
  ::equipped-weapons
  :<- [:equipped-sorted]
  :<- [::eq-proficiencies]
  :<- [::ability-modifiers]
  :<- [::proficiency-bonus]
  :<- [::damage-bonuses]
  :<- [::finesse-weapon-kinds]
  (fn [[all-equipped proficiencies modifiers
        proficiency-bonus dmg-bonuses
        finesse-weapon-kinds]]
    (let [{proficient-kinds :kinds
           proficient-cats :categories} proficiencies]
      (->> all-equipped
           (filter #(= :weapon (:type %)))
           (map
             (partial calculate-weapon
                      proficient-cats proficient-kinds
                      modifiers
                      proficiency-bonus dmg-bonuses
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

; ======= Spells ===========================================

(defn knowable-spell-counts-for
  [c modifiers]
  (let [{:keys [id level]} c
        {:keys [cantrips slots known]} (-> c :attrs :5e/spellcaster)

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
  :<- [::spellcaster-classes]
  :<- [::spellcasting-modifiers]
  (fn [[classes modifiers]]
    (reduce
      (fn [m c]
        (assoc m (:id c) (knowable-spell-counts-for c modifiers)))
      {}
      classes)))

(reg-sub
  ::knowable-spell-counts
  :<- [::knowable-spell-counts-by-class]
  (fn [counts [_ class-id]]
    (get counts class-id)))

(reg-sub
  ::prepared-spells-by-class
  :<- [::spellcaster-classes]
  :<- [:sheet-source]
  :<- [::spellcasting-modifiers]
  :<- [:options]
  (fn [[classes data-source modifiers options]]
    (when (seq classes)
      (reduce
        (fn [m c]
          (let [attrs (-> c :attrs :5e/spellcaster)
                {:keys [acquires?]} attrs
                spells-list (:spells attrs)
                extra-spells-list (:extra-spells attrs)

                ; if we acquire spells, the source list is still the same,
                ; but the we use the :acquires?-spells option-list to
                ; determine which are actually prepared (the normal :spells
                ; list just indicates which spells are *acquired*)
                spells-option (if acquires?
                                (:acquires?-spells attrs)
                                spells-list)
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
                extra-spells (or (get-in c [:lists extra-spells-list])
                                 (when (coll? extra-spells-list)
                                   (map (partial
                                          src/find-list-entity
                                          data-source)
                                        extra-spells-list)))
                ]

            ; TODO for :acquires? spellcasters, their
            ; cantrips are always prepared
            (assoc
              m (:id c)
              (->> (concat
                     (->> extra-spells
                          (map #(assoc % :always-prepared? true)))

                     ; only selected spells from the main list
                     (expand-list data-source spells-list
                                  (or (get options spells-option)
                                      [])))

                   (map #(assoc %
                                ::source (:id c)
                                :spell-mod (get modifiers (:id c))))

                   ; sort by level, then name
                   (sort-by (juxt :spell-level :name))))))
        {}
        classes))))

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
    (filter (case filter-type
              :bonus util/bonus-action?
              :reaction util/reaction?

              (if (number? filter-type)
                #(= filter-type (:level %))

                (throw (js/Error.
                         (str "Unknown spell filter-type:" filter-type)))))
            spells)))

(reg-sub
  ::combat-actions
  :<- [:classes]
  :<- [:sheet-source]
  (fn [[classes data-source] [_ filter-type]]
    (->> classes
         (mapcat
           (fn [c]
             (let [ids (keys (get-in c [:attrs filter-type]))]
               (map
                 (fn [id]
                   (or (get-in c [:features id])
                       (src/find-feature data-source id)))
                 ids))))
         (sort-by :name))))

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

(declare spell-slots)

; list of all spells on a given spell list for the given class,
; with `:prepared? bool` inserted as appropriate
(reg-sub
  ::preparable-spell-list

  (fn [[_ the-class list-id]]
    [(subscribe [:sheet-source])
     (subscribe [:options])
     (subscribe [::prepared-spells (:id the-class)])
     (subscribe [::highest-spell-level-for-class-id (:id the-class)])])

  (fn [[data-source options prepared-spells highest-spell-level]
       [_ the-class list-id]]
    (let [attrs (-> the-class :attrs :5e/spellcaster)
          is-acquired-list? (= (:acquires?-spells attrs)
                               list-id)
          prepared-set (->> prepared-spells
                            (map :id)
                            set)
          always-prepared-set (->> prepared-spells
                                   (filter :always-prepared?)
                                   (map :id)
                                   set)
          source (if is-acquired-list?
                   ; if we want to look at the :acquired? list, its
                   ; source is actually the selected from (:spells)
                   (expand-list data-source
                                (:spells attrs)
                                (get options (:spells attrs) #{}))

                   ; normal case:
                   (expand-list data-source list-id nil))]

      (->> source
           ; limit visible spells by those actually available
           ; (IE it must be of a level we can prepare)
           (filter #(<= (:spell-level %) highest-spell-level))

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
  ::spellcaster-classes
  :<- [:classes]
  :<- [:races]
  (fn [spellcaster-collections]
    (->> spellcaster-collections
         flatten
         (filter (fn [c]
                   (-> c :attrs :5e/spellcaster))))))

(reg-sub
  ::spellcasting-modifiers
  :<- [::abilities]
  :<- [::spellcaster-classes]
  (fn [[abilities classes]]
    (reduce
      (fn [m c]
        (let [spellcasting-ability (-> c
                                       :attrs
                                       :5e/spellcaster
                                       :ability)]
          (assoc m
                 (:id c)
                 (ability->mod
                   (get abilities spellcasting-ability)))))
      {}
      classes)))

(reg-sub
  ::spell-attack-bonuses
  :<- [::spellcasting-modifiers]
  :<- [::proficiency-bonus]
  (fn [[modifiers proficiency-bonus]]
    (reduce-kv
      (fn [m class-or-race-id modifier]
        (assoc m class-or-race-id (+ proficiency-bonus modifier)))
      {}
      modifiers)))

(defn- standard-spell-slots?
  [c]
  (= :standard (or (-> c
                       :attrs
                       :5e/spellcaster
                       :slots-type)
                   :standard)))

(defn spell-slots
  [spellcaster-classes]
  (if (= 1 (count spellcaster-classes))
    (let [c (first spellcaster-classes)
          level (:level c)
          spellcaster (-> c :attrs :5e/spellcaster)
          kind (:slots-type spellcaster :standard)
          label (:slots-label spellcaster std-slots-label)
          schedule (:slots spellcaster :standard)
          schedule (if (keyword? schedule)
                     (schedule spell-slot-schedules)
                     schedule)]
      {kind {:label label
             :slots (get schedule level)}})

    (let [std-level (apply
                      +
                      (->> spellcaster-classes
                           (filter standard-spell-slots?)
                           (map
                             (fn [c]
                               (let [mod (get-in
                                           c
                                           [:attrs
                                            :5e/spellcaster
                                            :multiclass-levels-mod]
                                           1)]
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
        (->> spellcaster-classes
             (filter (complement standard-spell-slots?))
             (map (fn [c]
                    (spell-slots [c]))))))))

(reg-sub
  ::spellcaster-classes-with-slots
  :<- [::spellcaster-classes]
  (fn [all-classes]
    (filter #(not= :none
                   (-> % :attrs :5e/spellcaster :slots))
            all-classes)))

(reg-sub
  ::spell-slots
  :<- [::spellcaster-classes-with-slots]
  spell-slots)

(reg-sub
  ::spell-slots-for-class-id
  (fn [[_ class-id]]
    (subscribe [::class-by-id class-id]))
  (fn [the-class]
    (spell-slots [the-class])))

(reg-sub
  ::highest-spell-level-for-class-id
  (fn [[_ class-id]]
    (subscribe [::spell-slots-for-class-id class-id]))
  (fn [spell-slots]
    (->> spell-slots

         ; only one slot type since there's only one class
         vals
         first

         :slots

         ; highest spell level available
         keys
         (apply max))))

(reg-sub
  ::spellcaster-slot-types
  :<- [::spellcaster-classes-with-slots]
  (fn [classes]
    (->> classes
         (filter (complement standard-spell-slots?))
         (map #(name
                 (get-in %
                         [:attrs
                          :5e/spellcaster
                          :slots-type])))
         set)))

(reg-sub
  ::spell-slots-used
  :<- [:limited-used]
  :<- [::spellcaster-slot-types]
  (fn [[used slot-types]]
    (reduce-kv
      (fn [m id used]
        (let [id-ns (namespace id)
              level (-> id name last int)]
          (cond
            (= "slots" id-ns)
            (assoc-in m [:standard level] used)

            (contains? slot-types id-ns)
            (assoc-in m [(keyword id-ns) level] used)

            :else m)))  ; ignore; unrelated
      {}
      used)))


; ======= builder-specific =================================

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
  (fn [[_ entity-id primary?]]
    [(subscribe [:class-features-with-options entity-id primary?])
     (subscribe [::highest-spell-level-for-class-id entity-id])])
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


; ======= etc ==============================================

(reg-sub
  ::currency
  :<- [:sheet]
  (fn [sheet _]
    (:currency sheet)))

; hacks?
(reg-sub
  ::features-for
  (fn [[_ sub-vec]]
    (subscribe sub-vec))
  (fn [sources]
    (->> sources
         (mapcat (comp vals :features))
         (filter :name)
         (remove :implicit?)
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

(reg-sub
  ::notes
  :<- [:sheet]
  (fn [sheet _]
    (get sheet :notes)))
