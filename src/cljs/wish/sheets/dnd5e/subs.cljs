(ns ^{:author "Daniel Leong"
      :doc "dnd5e.subs"}
  wish.sheets.dnd5e.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [wish.sources.core :refer [find-class find-race]]))

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
  ::max-hp
  :<- [:sheet]
  :<- [:classes]
  (fn [[sheet classes]]
    (apply +
           (->> classes
                (filter :primary?)
                first
                :attrs
                :5e/hit-dice)
           (->> sheet
                :hp-rolled))))

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
                     (println k v)
                     (= "proficiency" (namespace k)))))
         (map (comp keyword name first))
         (into #{}))))
