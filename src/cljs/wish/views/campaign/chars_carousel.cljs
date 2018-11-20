(ns ^{:author "Daniel Leong"
      :doc "campaign.chars-carousel"}
  wish.views.campaign.chars-carousel
  (:require [wish.util :refer [<sub >evt]]
            [wish.util.nav :as nav :refer [sheet-url]]
            [wish.views.error-boundary :refer [error-boundary]]
            [wish.views.widgets :refer [icon link link>evt]]))

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

(defn add-chars-overlay []
  (let [campaign-id (<sub [:active-sheet-id])]
    [:div.add-chars-overlay
     [:div.title
      "Add characters to "
      (<sub [:meta/name])]

     [:div.candidates
      (for [c (<sub [:campaign-members])]
        ^{:key (:id c)}
        [:div.character
         (:name c)

         [:input.invite-url
          {:type :text
           :value (nav/campaign-invite-url
                    campaign-id
                    (:id c))}
          ]
         ])

      (for [c (<sub [:campaign-add-char-candidates])]
        ^{:key (:id c)}
        [:div.character
         (:name c)

         [:div
          "INVITE (todo)"]
         ])]
     ]))

(defn chars-carousel [chars-card-view]
  (if-let [members (seq (<sub [:campaign-members]))]
    [:div.carousel-container
     [:div.carousel

      [link>evt {:> [:toggle-overlay [#'add-chars-overlay]]
                 :class "add-button"}
       (icon :add)]

      (for [c members]
        ^{:key (:id c)}
        [link {:href (sheet-url (:id c))
               :class "card"}
         [:div.card
          [char-sheet-loader
           (:id c)
           chars-card-view]]])]]

    [:div.empty-carousel
     "No characters in this campaign... yet!"]))
