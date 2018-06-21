(ns ^{:author "Daniel Leong"
      :doc "router"}
  wish.views.router
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [wish.util :refer [<sub >evt]]))

(defn router
  "Renders the current page, given a map
   of page-id to page render fn."
  [routes-map]
  (let [[page args] (<sub [:page])]
    (println "[router]" page args)
    [(get routes-map page) args]))


