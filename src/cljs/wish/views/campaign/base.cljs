(ns ^{:author "Daniel Leong"
      :doc "base"}
  wish.views.campaign.base
  (:require [spade.core :refer [defattrs]]
            [wish.style.flex :as flex]
            [wish.util.nav :refer [campaign-url]]
            [wish.util :refer [<sub]]
            [wish.views.campaign.chars-carousel :refer [chars-carousel]]
            [wish.views.campaign.workspace :refer [workspace]]
            [wish.views.error-boundary :refer [error-boundary]]
            [wish.views.widgets :refer [link]]))

(defn- campaign-sub-url [& sections]
  (apply campaign-url (<sub [:active-sheet-id]) sections))

(defattrs campaign-nav-style []
  (flex/create
    :center :horizontal
    {:background "#666666"
     :margin-bottom "8px"
     :padding "8px 0"
     :width "100%"})
  [:div.nav-link {:color "#f0f0f0"}])

(defn- campaign-link [current-section & {:keys [section label]}]
  (if (= current-section section)
    [:div.nav-link label]
    [link {:href (campaign-sub-url section)}
     label]))

(defn- campaign-nav [section]
  [:div (campaign-nav-style)
   [campaign-link section
    :label "Home"]
   [campaign-link section
    :section :notes
    :label "Notes"]
   [campaign-link section
    :section :spaces
    :label "Spaces"]])

(defn campaign-page
  [section & {:keys [char-card entity-card]}]
  [error-boundary
   [:div.campaign
    [campaign-nav section]

    [chars-carousel char-card]

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
