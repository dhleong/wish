(ns ^{:author "Daniel Leong"
      :doc "util"}
  wish.util
  (:require [reagent.dom :as reagent-dom]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]))

(def <sub (comp deref subscribe))
(def >evt dispatch)

(defn click>evt
  "Returns an on-click handler that dispatches the given event
  and prevents the default on-click events"
  [event]
  (fn [e]
    (.preventDefault e)
    (>evt event)))

(def is-ios?
  (memoize
    (fn is-ios? []
      (and (boolean js/navigator.platform)
           (re-find #"iPad|iPhone|iPod" js/navigator.platform)))))

(defn navigate!
  [& args]
  (let [evt (vec (cons :navigate! args))]
    (if (is-ios?)
      ; NOTE: on iOS we do some whacky shit to prevent awful flashes
      ;  when swiping back. hopefully there's a more efficient way
      ;  to do this, but for now... this works
      (do
        (dispatch-sync evt)
        (reagent-dom/force-update-all))

      ; When we don't have to worry about back-swipe, we can be more
      ;  relaxed about handling navigation
      (dispatch evt))))

