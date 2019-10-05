(ns ^{:author "Daniel Leong"
      :doc "base"}
  wish.views.campaign.base
  (:require [spade.core :refer [defattrs]]
            [wish.style.flex :as flex]
            [wish.views.campaign.chars-carousel :refer [chars-carousel]]
            [wish.views.campaign.workspace :refer [workspace]]
            [wish.views.error-boundary :refer [error-boundary]]))

(defattrs campaign-nav-style []
  (flex/create
    :center :horizontal
    {:background "#eee"
     :margin-bottom "8px"
     :padding "8px 0"
     :width "100%"}))

(defn- campaign-nav []
  [:div (campaign-nav-style)
   [:div.button "Notes"]
   [:div.button "Spaces"]])

(defn campaign-page
  [_section & {:keys [char-card entity-card]}]
  [error-boundary
   [:div.campaign
    [chars-carousel char-card]

    [campaign-nav]

    [workspace
     :entity-card entity-card]

    #_[:div.info
     "This is the campaign page."

     [:div.group
      [:a {:href "https://github.com/dhleong/wish/issues/69"
           :target '_blank}
       "More will be coming here"]
      "... eventually."]

     [:div.group
      "In the meantime, use the + button above to add characters to this campaign."]
     ]]])
