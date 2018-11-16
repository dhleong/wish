(ns ^{:author "Daniel Leong"
      :doc "campaign.chars-carousel"}
  wish.views.campaign.chars-carousel
  (:require [wish.util :refer [<sub >evt]]
            [wish.views.error-boundary :refer [error-boundary]]))

(defn- sheet-loader [sheet]
  [:div "Loading " (:name sheet) "..."])

(defn- sources-loader [sheet]
  [:div "Loading " (:name sheet) "..."])

(defn- char-sheet-loader
  [sheet-id content-fn]
  (let [sheet (<sub [:provided-sheet sheet-id])]
    (if (:sources sheet)
      (if-let [source (<sub [:sheet-source sheet-id])]
        ; sheet is ready; render!
        [error-boundary
         [content-fn sheet]]

        (do
          (>evt [:load-sheet-source! sheet (:sources sheet)])
          [sources-loader sheet]))

      ; either we don't have the sheet at all, or it's just
      ; a stub with no actual data; either way, load it!
      (do
        (>evt [:load-sheet! sheet-id])
        [sheet-loader sheet]))))

(defn chars-carousel [chars-card-view]
  (if-let [members (seq (<sub [:campaign-members]))]
    [:div.carousel-container
     [:div.carousel

      (for [c members]
        ^{:key (:id c)}
        [:div.card
         [char-sheet-loader
          (:id c)
          chars-card-view]])]]

    [:div.empty-carousel
     "No characters in this campaign... yet!"]))
