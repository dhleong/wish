(ns wish.style.components
  (:require [spade.core :refer [defclass defglobal]]
            [wish.style.media :as media]
            [wish.style.shared :as shared]))

(def disabled-button {:font-style 'italic
                      :color "rgba(1,1,1, 0.25) !important"
                      :cursor 'default})

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

   [:&.disabled disabled-button]

   ["&:not(.disabled):hover" {:background "#ccc"}
    (at-media media/dark-scheme
      {:background-color "#666666"})

    ; nested buttons that are disabled should have their
    ; backgrounds overwritten to match ours
    [:.button.disabled {:background "#ccc"}]]

   ["&:not(.disabled):active" {:background "#666666"}
    (at-media media/dark-scheme
      {:background "#333"})

    ; nested buttons that are disabled should have their
    ; backgrounds overwritten to match ours
    [:.button.disabled {:background "#666666"}]]

   (at-media media/dark-scheme
     [:& {:background-color "#555"}])

   ])
