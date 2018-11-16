(ns ^{:author "Daniel Leong"
      :doc "Campaign-viewer for D&D 5e"}
  wish.sheets.dnd5e.campaign
  (:require [wish.views.campaign.chars-carousel :refer [chars-carousel]]))

(defn char-card [c]
  [:div.character
   (:name c)])

(defn view
  [section]
  [:div
   [chars-carousel char-card]
   "TODO Campaign"])
