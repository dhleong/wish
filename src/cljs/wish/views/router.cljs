(ns ^{:author "Daniel Leong"
      :doc "router"}
  wish.views.router
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [wish.util :refer [<sub >evt]]))

(defn- pick-page-title []
  (let [current-sheet-name (:name (<sub [:sheet-meta]))]
    (cond
      ; are we looking at a sheet?
      current-sheet-name (str current-sheet-name " [WISH]")

      ; default:
      :else "WISH")))

(defn router
  "Renders the current page, given a map
   of page-id to page render fn."
  [routes-map]
  (let [[page args] (<sub [:page])]
    (>evt [:title! (pick-page-title)])

    (println "[router]" page args)
    [(get routes-map page) args]))


