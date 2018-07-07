(ns ^{:author "Daniel Leong"
      :doc "sheet-browser"}
  wish.views.sheet-browser
  (:require [wish.util :refer [<sub]]
            [wish.util.nav :refer [sheet-url]]
            [wish.views.widgets :refer [link]]))

(defn page []
  [:div "Sheets"
   (when (<sub [:providers-listing?])
     [:div.loading "Loading..."])

   [:ul
    (for [s (<sub [:known-sheets])]
      ^{:key (:id s)}
      [:li.sheet-link
       [link {:href (sheet-url (:id s))}
        (:name s)]])]
   [:div
    [link {:href "/sheets/new"}
     "Create a new sheet"]]])
