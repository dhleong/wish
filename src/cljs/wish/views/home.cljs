(ns ^{:author "Daniel Leong"
      :doc "home"}
  wish.views.home
  (:require [wish.views.widgets :refer [link]]
            [wish.util :refer [<sub click>evt]]))

(defn home []
  (let [can-use-sheets? (<sub [:any-storable-provider?])
        any-sheets-loaded? (seq (<sub [:filtered-known-sheets]))
        demo-state (<sub [:provider-state :demo])]
    [:div
     [:h3 "Welcome to Wish!"]
     [:p "It's not very pretty around here just yet, but feel free to look around."]

     [:div.group
      [:div
       [link {:href "/sheets/new"}
        "New sheet"]]

     (when (or can-use-sheets?
               any-sheets-loaded?)
       [:div
        [link {:href "/sheets"}
         "Open a sheet"]] )

     [:div.group
      [:div
       [link {:href "/campaigns/new"}
        "New Campaign"]]

      (when (or can-use-sheets?
                any-sheets-loaded?)
        [:div
         [link {:href "/campaigns"}
          "Open a Campaign"]])]

     [:div.group
      [:div
       [link {:href "/providers/gdrive/config"}
        "Configure Google Drive"]]

      (when-not can-use-sheets?
        [:div.explanation
         "Wish does not store your sheets directly, but instead lets you connect Providers, like Google Drive, to ensure you have full control of your data. In order to create or view a sheet, you must configure a Provider."])
      ]

     [:div.group
      [:h3 "Demos"]

      (when-not can-use-sheets?
        [:div
         "Want to give Wish a test drive before connecting a Provider? Sure thing? Enable or disable demo sheets here:"])

      [:div.explanation
       "Demo sheets start with some fixed values, but you can freely modify them to play with any available classes and features. Changes will " [:i "not"] " actually be persisted."]

      [link {:on-click (click>evt
                         (if (= :ready demo-state)
                           [:providers/disconnect! :demo]
                           [:providers/connect! :demo]))}
       (if (not= :ready demo-state)
         "Show Demo Sheets"
         "Hide Demo Sheets")
       ]]
     ]]))
