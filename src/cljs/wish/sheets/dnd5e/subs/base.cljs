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

