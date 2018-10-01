(ns ^{:author "Daniel Leong"
      :doc "update-notifier"}
  wish.views.update-notifier
  (:require [wish.util :refer [>evt <sub]]
            [wish.views.widgets :refer [icon link>evt]]))

(defn update-notifier []
  (when (<sub [:update-available?])
    [:div.update-notifier
     [:div.ignore
      [link>evt {:class "link"
                 :> [:ignore-latest-update]}
       (icon :close)]]
     [:div.content "New version of WISH available!"]
     [:div.update
      [link>evt [:update-app]
       "Update"]]]))
