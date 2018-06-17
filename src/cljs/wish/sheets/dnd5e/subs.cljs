(ns ^{:author "Daniel Leong"
      :doc "dnd5e.subs"}
  wish.sheets.dnd5e.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wish.sources.core :refer [expand-list find-class find-race]]
            [wish.sheets.dnd5e.util :refer [ability->mod]]
            [wish.util :refer [invoke-callable]]))

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
  ::hp
  :<- [:sheet]
  :<- [::abilities]
  :<- [::total-level]
  :<- [:limited-used]
  (fn [[sheet abilities total-level limited-used-map]]
    (let [max-hp (apply +
                        (* total-level
                           (->> abilities
                                :con
                                ability->mod))
                        (->> sheet
                             :hp-rolled))
          used-hp (or (:hp#uses limited-used-map)
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
  (fn [[classes data-source]]
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
                     #(assoc % ::source source)
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

; TODO races also have their own spellcasting ability modifier
(reg-sub
  ::spell-attack-bonuses
  :<- [::abilities]
  :<- [:classes]
  :<- [::proficiency-bonus]
  (fn [[abilities classes proficiency-bonus]]
    (->> classes
         (filter (fn [c]
                   (-> c :attrs :5e/spellcaster)))
         (map (fn [c]
                (let [spellcasting-ability (-> c
                                               :attrs
                                               :5e/spellcaster
                                               :ability)]
                  [(:id c) (+ proficiency-bonus
                              (ability->mod
                                (get abilities spellcasting-ability)))])))
         (into {}))))
