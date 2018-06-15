(ns ^{:author "Daniel Leong"
      :doc "DND 5e sheet"}
  wish.sheets.dnd5e
  (:require [clojure.string :as str]
            [wish.util :refer [<sub]]
            [wish.sheets.dnd5e.subs :as dnd5e]))

(defn hp
  []
  (let [sheet (<sub [:sheet])
        max-hp (<sub [::dnd5e/max-hp])]
    [:div.hp "HP"
     [:div.now (:hp sheet)]
     [:div.max  (str "/" max-hp)]]))

(defn header
  []
  (let [common (<sub [:sheet-meta])
        sheet (<sub [:sheet])
        classes (<sub [:classes])]
    [:div.header "D&D"
     [:div.name (:name common)]
     ; TODO levels
     [:div.classes (->> classes
                        (map (fn [c]
                               (str (-> c :data :name) " " (:level c))))
                        (str/join " / "))]
     [:div.race (:name (<sub [:race]))]
     [hp]]))

(defn sheet
  []
  [:div
   [header]
   ])
