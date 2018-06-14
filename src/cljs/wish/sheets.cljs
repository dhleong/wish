(ns ^{:author "Daniel Leong"
      :doc "sheets"}
  wish.sheets
  (:require [wish.sheets.dnd5e :as dnd5e]
            [wish.util :refer [<sub >evt]]))

(def sheets
  {:dnd5e {:name "D&D 5E"
           :fn #'dnd5e/sheet}})

(defn sheet-loader []
  [:div "Loading..."])

(defn sheet-unknown [kind]
  [:div (str "`" kind "`") " is not a type of sheet we know about"])

(defn viewer
  [[kind sheet-id]]
  (if-let [info (get sheets (keyword kind))]
    (if-let [sheet (<sub [:provided-sheets sheet-id])]
      ; sheet is ready; render!
      [(:fn info)]

      (do
        (>evt [:load-sheet! sheet-id])
        [sheet-loader]))

    ; unknown sheet kind
    [sheet-unknown kind]))
