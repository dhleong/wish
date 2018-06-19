(ns ^{:author "Daniel Leong"
      :doc "sheets"}
  wish.sheets
  (:require [wish.sheets.dnd5e :as dnd5e]
            [wish.sheets.dnd5e.util :as dnd5e-util]
            [wish.util :refer [<sub >evt]]))

; TODO we could use code splitting here to avoid loading
; sheet templates that we don't care about
(def sheets
  {:dnd5e {:name "D&D 5E"
           :fn #'dnd5e/sheet

           ; Function for post-processing entities,
           ;  IE: applying :attr side-effects.
           ; post-process functions should accept
           ;  [entity, data-source, entity-kind]
           :post-process dnd5e-util/post-process
           }})

(defn post-process
  [entity sheet-kind data-source entity-kind]
  (if-let [processor (get-in sheets [sheet-kind :post-process])]
    (processor entity data-source entity-kind)

    ; no processor for this sheet; pass through
    entity))

(defn sheet-loader []
  [:div "Loading..."])

(defn sources-loader
  [sheet]
  [:div "Loading data for " (:name sheet) "..."])

(defn sheet-unknown [kind]
  [:div (str "`" kind "`") " is not a type of sheet we know about"])

(defn viewer
  [[kind sheet-id]]
  (if-let [sheet-info (get sheets (keyword kind))]
    (if-let [sheet (<sub [:provided-sheet sheet-id])]
      (if-let [source (<sub [:sheet-source sheet-id])]
        ; sheet is ready; render!
        [(:fn sheet-info)]

        (do
          (>evt [:load-sheet-source! sheet-id (:sources sheet)])
          [sources-loader sheet]))

      (do
        (>evt [:load-sheet! sheet-id])
        [sheet-loader]))

    ; unknown sheet kind
    [sheet-unknown kind]))
