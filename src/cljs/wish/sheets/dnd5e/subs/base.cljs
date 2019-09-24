(ns wish.sheets.dnd5e.subs.base
  (:require [re-frame.core :as rf :refer [reg-sub]]))

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
  :wish.sheets.dnd5e.subs/proficiency-bonus
  :<- [:total-level]
  (fn [total-level _]
    (level->proficiency-bonus total-level)))

; returns a set of ability ids
(reg-sub
  :wish.sheets.dnd5e.subs/save-proficiencies
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

; returns a collection of feature ids
(reg-sub
  :wish.sheets.dnd5e.subs/all-proficiencies
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

