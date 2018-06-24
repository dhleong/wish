(ns ^{:author "Daniel Leong"
      :doc "dnd5e.subs"}
  wish.sheets.dnd5e.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wish.sources.core :refer [expand-list find-class find-race]]
            [wish.sheets.dnd5e.util :refer [ability->mod]]
            [wish.util :refer [invoke-callable]]))

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
           ; TODO how should classes do this?
           [])))

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
  (fn [items]
    (remove
      :implicit?
      items)))

(reg-sub
  ::rolled-hp
  :<- [:sheet]
  (fn [sheet [_ ?path]]
    (get-in sheet (concat
                    [:hp-rolled]
                    ?path))))

(reg-sub
  ::max-hp
  :<- [::rolled-hp]
  :<- [::abilities]
  :<- [::total-level]
  (fn [[rolled-hp abilities total-level]]
    (apply +
           (* total-level
              (->> abilities
                   :con
                   ability->mod))
           (->> rolled-hp
                vals
                flatten))))

(reg-sub
  ::hp
  :<- [::max-hp]
  :<- [:limited-used]
  (fn [[max-hp limited-used-map]]
    (let [used-hp (or (:hp#uses limited-used-map)
                      0)]
      [(- max-hp used-hp) max-hp])))

; returns a set of skill ids
(reg-sub
  ::skill-proficiencies
  :<- [:classes]
  (fn [classes _]
    ; TODO do any races provide skill proficiency?
    (->> classes
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
  :<- [::total-level]
  (fn [total-level _]
    (level->proficiency-bonus total-level)))

(reg-sub
  ::total-level
  :<- [:classes]
  (fn [classes _]
    (apply + (map :level classes))))

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
                       :to-hit (if (:versatile? s)
                                   (+ proficiency-bonus
                                      (max (:str modifiers)
                                           (:dex modifiers)))
                                   (+ proficiency-bonus (:str modifiers)))
                       :dmg (invoke-callable
                               s :dice
                               :modifiers modifiers))))

         first)))

; ======= Spells ===========================================

; TODO filter by selected
(reg-sub
  ::class-spells
  :<- [:classes]
  :<- [:sheet-source]
  :<- [::spellcasting-modifiers]
  (fn [[classes data-source modifiers]]
    (->> classes
         (mapcat (fn [c]
                   (when-let [attrs (-> c :attrs :5e/spellcaster)]
                     [{:list-id (:spells attrs)
                       ::source (:id c)}
                      {:list-id (:extra-spells attrs)
                       ::source (:id c)}])))

         (filter identity)

         ; TODO provide options in case the list is
         ; a feature with options
         (mapcat (fn [{:keys [list-id] ::keys [source]}]
                   (map
                     #(assoc % ::source source
                             :spell-mod (get modifiers source))
                     (expand-list data-source list-id nil)))))))


(reg-sub
  ::race-spells
  :<- [:race]
  :<- [:sheet-source]
  (fn [[race source]]
    ; TODO
    []))

(reg-sub
  ::spell-attacks
  :<- [::spell-attack-bonuses]
  :<- [::class-spells]
  :<- [::race-spells]
  (fn [[bonuses & spell-lists]]
    (->> spell-lists
         flatten
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
  (fn [all-classes]
    (filter (fn [c]
              (-> c :attrs :5e/spellcaster))
            all-classes)))

; TODO races also have their own spellcasting ability modifier
(reg-sub
  ::spellcasting-modifiers
  :<- [::abilities]
  :<- [::spellcaster-classes]
  (fn [[abilities classes]]
    (->> classes
         (map (fn [c]
                (let [spellcasting-ability (-> c
                                               :attrs
                                               :5e/spellcaster
                                               :ability)]
                  [(:id c) (ability->mod
                             (get abilities spellcasting-ability))])))
         (into {}))))

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
  ::spell-slots
  :<- [::spellcaster-classes]
  spell-slots)

(reg-sub
  ::spellcaster-slot-types
  :<- [::spellcaster-classes]
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
