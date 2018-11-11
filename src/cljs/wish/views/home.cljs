(ns ^{:author "Daniel Leong"
      :doc "home"}
  wish.views.home
  (:require [wish.views.widgets :refer [link]]
            [wish.util :refer [<sub]]))

(defn home []
  (let [can-use-sheets? (<sub [:any-storable-provider?])]
    [:div
     [:h3 "Welcome to Wish!"]
     [:p "It's not very pretty around here just yet, but feel free to look around."]

     [:div
      [link {:href "/sheets/new"}
       "New sheet"]]

     (when can-use-sheets?
       [:div
        [link {:href "/sheets"}
         "Open a sheet"]] )

     [:div
      [link {:href "/providers/gdrive/config"}
       "Configure Google Drive"]]

     (when-not can-use-sheets?
       [:div.explanation
        "Wish does not store your sheets directly, but instead lets you connect Providers, like Google Drive, to ensure you have full control of your data. In order to create or view a sheet, you must configure a Provider."])
     ]))
