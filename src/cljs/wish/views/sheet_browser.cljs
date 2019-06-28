(ns ^{:author "Daniel Leong"
      :doc "sheet-browser"}
  wish.views.sheet-browser
  (:require [reagent-forms.core :refer [bind-fields]]
            [wish.providers :as providers]
            [wish.util :refer [>evt <sub]]
            [wish.util.nav :refer [sheet-url]]
            [wish.views.widgets :refer [icon link]]))

(defn page []
  [:div
   [:h3
    [link {:href "/"}
     (icon :home)]
    "Sheets"]

   [bind-fields
    [:div

     [:input {:field :checkbox
              :id :mine?}]
     [:label {:for :mine?}
      "My Sheets"]

     [:input {:field :checkbox
              :id :shared?}]
     [:label {:for :shared?}
      "Shared Sheets"]]

    {:get #(get-in (<sub [:sheets-filters]) %)
     :save! #(>evt [:filter-sheets (first %1) %2])}]

   [:div.new-sheet-link
    [link {:href "/sheets/new"}
     "Create a new sheet"]]

   (let [listing? (<sub [:providers-listing?])
         known-sheets (<sub [:filtered-known-sheets])
         filters (<sub [:sheets-filters])
         any-provider-ready? (<sub [:any-storable-provider?])]

     [:<>

      (when listing?
        [:div.loading "Loading..."])

      (cond
        (seq known-sheets)
        [:ul
         (for [s known-sheets]
           ^{:key (:id s)}
           [:li.sheet-link
            [link {:href (sheet-url (:id s))}
             (:name s)]])]

        ; no known sheets, but we're still loading
        listing?
        nil

        ; no sheets at all, and no providers
        (not any-provider-ready?)
        [:div
         [:div "No sheets available."]
         [:div
          "You must configure a provider like "
          [link {:href "/providers/gdrive/config"}
           "Google Drive"]
          " to store your sheets."]]

        ; providers, but no sheets *at all*
        (nil? (seq (<sub [:known-sheets])))
        [:div
         [:div "No sheets created... yet!"]]

        ; we have sheets, but none shared
        (not (:mine? filters))
        [:div
         [:div "No shared sheets available."]
         [:div.explanation
          "If you have disconnected and reconnected a provider, it's possible
           that some permissions were temporarily lost. Some providers, like
           Google Drive, need extra permission for us to be able to find sheets
           that have been shared with you."]

         [:div.explanation
          "One of the following options may help:"]

         [providers/error-resolver-view :no-shared-sheets]]

        :else
        [:div "No sheets available with these filters."])])])
