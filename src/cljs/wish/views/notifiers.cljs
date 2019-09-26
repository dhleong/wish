(ns ^{:author "Daniel Leong"
      :doc "notifiers"}
  wish.views.notifiers
  (:require [wish.util :refer [<sub click>evts]]
            [wish.events :as events]
            [wish.views.widgets :refer [icon link>evt]]))

(defn notifier
  [{:keys [id
           dismiss-event
           content
           action-label
           action-event
           action-dismiss?]
    :or {action-dismiss? true}}]
  [:div.notifier
   (when dismiss-event
     [:div.ignore
      [link>evt {:class "link"
                 :> dismiss-event}
       (icon :close)]])

   [:div.content content]

   (when action-event
     [:div.action
      [:a {:href "#"
           :on-click (click>evts
                       action-event
                       (when action-dismiss?
                         [::events/remove-notify! id]))}
       action-label]])])

(defn update-notifier []
  (when (<sub [:update-available?])
    [notifier
     {:dismiss-event [:ignore-latest-update]
      :content "New version of WISH available!"
      :action-label "Update"
      :action-event [:update-app]}]))

(defn notifiers []
  [:div.notifiers
   (when-let [notifications (seq (<sub [:notifications]))]
     (for [{:keys [id] :as n} notifications]
       ^{:key id}
       [notifier n]))

   [update-notifier]])
