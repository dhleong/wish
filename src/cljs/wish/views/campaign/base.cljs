(ns ^{:author "Daniel Leong"
      :doc "base"}
  wish.views.campaign.base
  (:require [wish.views.campaign.chars-carousel :refer [chars-carousel]]
            [wish.views.campaign.workspace :refer [workspace]]
            [wish.views.error-boundary :refer [error-boundary]]))

(defn campaign-page
  [_section & {:keys [char-card entity-card]}]
  [error-boundary
   [:div.campaign
    [chars-carousel char-card]
    [workspace
     :entity-card entity-card]

    [:div.info
     "This is the campaign page."

     [:div.group
      [:a {:href "https://github.com/dhleong/wish/issues/69"
           :target '_blank}
       "More will be coming here"]
      "... eventually."]

     [:div.group
      "In the meantime, use the + button above to add characters to this campaign."]
     ]]])
