(ns ^{:author "Daniel Leong"
      :doc "campaign.chars-carousel"}
  wish.views.campaign.chars-carousel
  (:require [wish.util :refer [<sub >evt]]
            [wish.util.nav :as nav :refer [sheet-url]]
            [wish.views.error-boundary :refer [error-boundary]]
            [wish.views.widgets :refer [icon link link>evt]]
            [wish.views.campaign.events :as events]
            [wish.views.campaign.subs :as subs]))

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
  (let [campaign-id (<sub [:active-sheet-id])
        campaign-name (<sub [:meta/name])]
    [:div.add-chars-overlay
     [:div.title
      "Add characters to " campaign-name]

     [:div.candidates
      (for [c (<sub [::subs/campaign-members])]
        ^{:key (:id c)}
        [:div.character
         [:div.name (:name c)]

         [:input.invite-url
          {:type :text
           :read-only true
           :value (nav/campaign-invite-url
                    campaign-id
                    (:id c)
                    campaign-name)}]

         [:div.remove
          [link>evt [::events/remove-player (:id c)]
           (icon :close)]]
         ])

      (for [c (<sub [::subs/add-char-candidates])]
        ^{:key (:id c)}
        [:div.character
         [:div.name (:name c)]

         [:div.add
          [link>evt [::events/add-player (:id c)]
           "Add to campaign"]]
         ])]
     ]))

(defn chars-carousel [chars-card-view]
  (if-let [members (seq (<sub [::subs/campaign-members]))]
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
