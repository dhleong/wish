(ns ^{:author "Daniel Leong"
      :doc "sheet-browser"}
  wish.views.sheet-browser
  (:require [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
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

   (when (<sub [:providers-listing?])
     [:div.loading "Loading..."])

   [:ul
    (for [s (<sub [:filtered-known-sheets])]
      ^{:key (:id s)}
      [:li.sheet-link
       [link {:href (sheet-url (:id s))}
        (:name s)]])] ])
