(ns ^{:author "Daniel Leong"
      :doc "home"}
  wish.views.home
  (:require [wish.views.widgets :refer [link]]))

(defn home []
  [:div
   [:h3 "Welcome to Wish!"]
   [:p "It's not very pretty around here just yet, but feel free to look around."]
   [:div
    [link {:href "/sheets/new"}
     "New sheet"]]
   [:div
    [link {:href "/sheets"}
     "Open a sheet"]]
   [:div
    [link {:href "/providers/gdrive/config"}
     "Configure Google Drive"]]])
