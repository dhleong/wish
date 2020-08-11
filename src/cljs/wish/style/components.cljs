(ns wish.style.components
  (:require [spade.core :refer [defclass defglobal]]
            [wish.style.media :as media]
            [wish.style.shared :as shared]))

(defclass unselectable []
  shared/unselectable)

(defclass clickable []
  {:composes [(unselectable)]}
  [:& shared/clickable])

(defglobal button
  [:.button (merge
              shared/clickable
              shared/unselectable
              {:border-radius "2px"
               :background "#999"
               :color "#fff"
               :margin [[0 "8px"]]
               :padding "8px"})

   [:&:hover {:background "#ccc"}

    ; nested buttons that are disabled should have their
    ; backgrounds overwritten to match ours
    [:.button.disabled {:background "#ccc"}]]

   [:&:active {:background "#666666"}
    ; nested buttons that are disabled should have their
    ; backgrounds overwritten to match ours
    [:.button.disabled {:background "#666666"}]]

   (at-media media/dark-scheme
     [:& {:background-color "#555"}
      [:&:hover {:background-color "#666666"}]])

   ])
