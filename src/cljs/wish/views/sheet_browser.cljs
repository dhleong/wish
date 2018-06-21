(ns ^{:author "Daniel Leong"
      :doc "sheet-browser"}
  wish.views.sheet-browser
  (:require [wish.util :refer [<sub]]
            [wish.views.widgets :refer [link]]))

(defn page []
  [:div "Sheets"
   (when (<sub [:providers-listing?])
     [:div.loading "Loading..."])
   [:ul
    (for [s (<sub [:known-sheets])]
      ^{:key (:id s)}
      [:li.sheet-link
       (let [provider (namespace (:id s))
             sheet-id (name (:id s))]
         [link {:href (str "/sheets/" provider "/" sheet-id)}
          (:name s)])])]])
