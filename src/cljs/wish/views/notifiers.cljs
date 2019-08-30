(ns ^{:author "Daniel Leong"
      :doc "notifiers"}
  wish.views.notifiers
  (:require [wish.util :refer [<sub]]
            [wish.views.widgets :refer [icon link>evt]]))

(defn notifier
  [& {:keys [ignore-event
             content
             action-label
             action-event]}]
  [:div.notifier
   (when ignore-event
     [:div.ignore
      [link>evt {:class "link"
                 :> ignore-event}
       (icon :close)]])

   [:div.content content]

   (when action-event
     [:div.action
      [link>evt action-event
       action-label]])])

(defn update-notifier []
  (when (<sub [:update-available?])
    [notifier
     :ignore-event [:ignore-latest-update]
     :content "New version of WISH available!"
     :action-label "Update"
     :action-event [:update-app]]))

(defn notifiers []
  [:div.notifiers
   (when-let [notifications (seq (<sub [:notifications]))]
     (for [{:keys [id content dismiss-event]} notifications]
       ^{:key id}
       [notifier
        :content content
        :ignore-event dismiss-event]))

   [update-notifier]])
