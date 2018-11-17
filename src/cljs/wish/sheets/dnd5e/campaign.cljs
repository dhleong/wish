(ns ^{:author "Daniel Leong"
      :doc "Campaign-viewer for D&D 5e"}
  wish.sheets.dnd5e.campaign
  (:require [wish.views.campaign.chars-carousel :refer [chars-carousel]]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.util :refer [<sub >evt]]))

(defn char-card [{:keys [id] :as c}]
  [:div.character
   (:name c)
   (str (<sub [::subs/hp id]))])

(defn view
  [section]
  [:div
   [chars-carousel char-card]
   "TODO Campaign"])
