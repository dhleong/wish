(ns ^{:author "Daniel Leong"
      :doc "campaign-browser"}
  wish.views.campaign-browser
  (:require [wish.util :refer [<sub]]
            [wish.util.nav :refer [campaign-url]]
            [wish.views.widgets :refer [icon link]]))

(defn page []
  [:div
   [:h3
    [link {:href "/"}
     (icon :home)]
    "Campaigns"]

   [:div.new-sheet-link
    [link {:href "/campaigns/new"}
     "Create a new Campaign"]]

   (when (<sub [:providers-listing?])
     [:div.loading "Loading..."])

   [:ul
    (for [s (<sub [:known-campaigns])]
      ^{:key (:id s)}
      [:li.sheet-link
       [link {:href (campaign-url (:id s))}
        (:name s)]])] ])
