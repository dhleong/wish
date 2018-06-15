(ns ^{:author "Daniel Leong"
      :doc "dnd5e.subs"}
  wish.sheets.dnd5e.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [wish.sources.core :refer [find-class find-race]]))

(reg-sub
  ::max-hp
  :<- [:sheet]
  :<- [:classes]
  (fn [[sheet classes]]
    (apply +
           (->> classes
                (filter :primary?)
                first
                :data
                :attrs
                :5e/hit-dice)
           (->> sheet
                :hp-rolled))))
