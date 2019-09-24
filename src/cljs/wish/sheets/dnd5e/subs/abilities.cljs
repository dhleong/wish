(ns wish.sheets.dnd5e.subs.abilities
  (:require [re-frame.core :as rf :refer [reg-sub]]
            [wish.sheets.dnd5e.data :as data]
            [wish.sheets.dnd5e.util :as util :refer [ability->mod
                                                     mod->str]]
            [wish.sheets.dnd5e.subs.base]
            [wish.subs-util :refer [reg-id-sub]]))


; ======= abilities =======================================

(reg-id-sub
  ::raw
  :<- [:meta/sheet]
  (fn [sheet]
    (:abilities sheet)))

; NOTE: we compute these buffs by hand because we (potentially) need the
; dependent sub to compute other buffs
(reg-id-sub
  ::improvements
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
  ::racial
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
  ::base
  :<- [::raw]
  :<- [::racial]
  :<- [::improvements]
  (fn [[abilities race improvements]]
    (merge-with +
                abilities
                race
                improvements)))

(reg-id-sub
  ::all
  :<- [::base]
  :<- [:meta/sheet]
  (fn [[base sheet]]
    (merge-with +
                base
                (:ability-tmp sheet))))

(reg-id-sub
  ::modifiers
  :<- [::all]
  (fn [abilities]
    (reduce-kv
     (fn [m ability score]
       (assoc m ability (ability->mod score)))
     {}
     abilities)))

(reg-id-sub
  ::saves
  :<- [::modifiers]
  :<- [:wish.sheets.dnd5e.subs/proficiency-bonus]
  :<- [:wish.sheets.dnd5e.subs/save-proficiencies]
  :<- [:wish.sheets.dnd5e.subs/buffs :saves]
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
  ::info
  :<- [::all]
  :<- [::base]
  :<- [::modifiers]
  :<- [:wish.sheets.dnd5e.subs/save-proficiencies]
  :<- [::saves]
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


; ======= skills ==========================================

; returns a set of skill ids
(reg-sub
  ::skill-proficiencies
  :<- [:wish.sheets.dnd5e.subs/all-proficiencies]
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

(reg-id-sub
  ::skill-info
  :<- [::modifiers]
  :<- [::skill-expertise]
  :<- [::skill-proficiencies]
  :<- [::skill-half-proficiencies]
  :<- [:wish.sheets.dnd5e.subs/proficiency-bonus]
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
