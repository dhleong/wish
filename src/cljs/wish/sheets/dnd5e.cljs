(ns ^{:author "Daniel Leong"
      :doc "DND 5e sheet"}
  wish.sheets.dnd5e
  (:require [wish.util :refer [<sub]]))

(defn header
  []
  (let [common (<sub [:sheet])
        sheet (<sub [:sheet-data])]
    [:div "D&D"
     [:div.name (:name common)]
     [:div.race (:name (<sub [:race]))]]))

(defn sheet
  []
  [:div
   [header]
   ])
