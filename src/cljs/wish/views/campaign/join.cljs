(ns ^{:author "Daniel Leong"
      :doc "Join a campaign as a player"}
  wish.views.campaign.join
  (:require [wish.util :refer [<sub >evt click>evts]]
            [wish.util.nav :refer [sheet-url]]
            [wish.views.campaign.events :as events]))

(defn page [[campaign-id sheet-id ?campaign-name]]
  [:div
   [:h3 "You've been invited to " (or ?campaign-name
                                      "a campaign")]

   [:div.explanation
    "This does not automatically grant the DM permission to modify your sheet,
     nor does it grant them permission to read it. You will need to use the
     sharing settings at the top of your sheet to grant whatever permissions
     you feel necessary. They must be able to at least read your sheet for
     there to be any benefit in joining the campaign, however!"]

   [:div
    [:h4 "Would you like to join?"]

    [:div.button {:on-click (click>evts
                              [::events/join-campaign campaign-id ?campaign-name]
                              [:nav/replace! (sheet-url sheet-id)])}
     "Yes! Join " (or ?campaign-name
                      "the campaign")]
    ]
   ])
