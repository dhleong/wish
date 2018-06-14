(ns ^{:author "Daniel Leong"
      :doc "DND 5e sheet"}
  wish.sheets.dnd5e
  (:require [wish.util :refer [<sub]]))

(defn header
  []
  (let [sheet (<sub [:sheet-data])]
    [:div "D&D"
     [:div.name (:name sheet)]
     [:div.race (:name (<sub [:race]))]]))

(defn sheet
  []
  [:div
   [header]
   ])
