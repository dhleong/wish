(ns ^{:author "Daniel Leong"
      :doc "home"}
  wish.views.home
  (:require [wish.views.widgets :refer [link]]))

(defn home []
  [:div
   [:div "Hi!"]
   [link {:href "/sheets/dnd5e/dummy/my-sheet-id"}
    "Open sheet"]
   [:div
    [link {:href "/providers/gdrive/config"}
     "Configure Google Drive"]]])
