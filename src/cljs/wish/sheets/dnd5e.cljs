(ns ^{:author "Daniel Leong"
      :doc "DND 5e sheet"}
  wish.sheets.dnd5e
  (:require [clojure.string :as str]
            [wish.util :refer [<sub]]))

(defn header
  []
  (let [common (<sub [:sheet])
        sheet (<sub [:sheet-data])]
    [:div.header "D&D"
     [:div.name (:name common)]
     ; TODO levels
     [:div.classes (->> (<sub [:classes])
                        (map (fn [c]
                               (str (-> c :data :name) " " (:level c))))
                        (str/join " / "))]
     [:div.race (:name (<sub [:race]))]]))

(defn sheet
  []
  [:div
   [header]
   ])
