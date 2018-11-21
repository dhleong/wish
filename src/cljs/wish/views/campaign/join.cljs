(ns ^{:author "Daniel Leong"
      :doc "Join a campaign as a player"}
  wish.views.campaign.join
  (:require [wish.util :refer [<sub >evt]]))

(defn page [[campaign-id sheet-id ?campaign-name]]
  [:div
   [:h3 "You've been invited to " (or ?campaign-name
                                      "a campaign")]
   ])
