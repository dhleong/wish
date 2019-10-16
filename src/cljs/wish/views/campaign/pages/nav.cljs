(ns wish.views.campaign.pages.nav
  (:require [spade.core :refer [defattrs]]
            [wish.style.flex :as flex]
            [wish.style.media :as media]
            [wish.util.nav :refer [campaign-url]]
            [wish.util :refer [<sub]]
            [wish.views.widgets :refer [link]]))

(defn- campaign-sub-url [& sections]
  (apply campaign-url (<sub [:active-sheet-id]) sections))

(defattrs campaign-nav-style []
  (flex/create)

  (at-media media/smartphones
    (flex/create
      :center :horizontal))

  [:div.nav-link {:color "#f0f0f0"}])

(defn- campaign-link [current-section & {:keys [section label]}]
  (if (= current-section section)
    [:div.nav-link label]
    [link {:href (campaign-sub-url section)}
     label]))

(defn campaign-nav [section]
  [:div (campaign-nav-style)
   [campaign-link section
    :label "Home"]
   [campaign-link section
    :section :notes
    :label "Notes"]
   [campaign-link section
    :section :spaces
    :label "Spaces"]])
