(ns ^{:author "Daniel Leong"
      :doc "home"}
  wish.views.home
  (:require [wish.views.widgets :refer [link]]))

(defn home []
  [:div
   [:div "Hi!"]
   [:div
    [link {:href "/sheets/dnd5e/dummy/my-sheet-id"}
     "Open dummy test sheet"]]
   [:div
    [link {:href "/sheets/new"}
     "New sheet"]]
   [:div
    [link {:href "/sheets"}
     "Open a sheet"]]
   [:div
    [link {:href "/providers/gdrive/config"}
     "Configure Google Drive"]]])
