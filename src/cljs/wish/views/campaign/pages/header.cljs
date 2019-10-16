(ns wish.views.campaign.pages.header
  (:require [spade.core :refer [defattrs]]
            [wish.style.flex :as flex]
            [wish.style.media :as media]
            [wish.views.campaign.pages.nav :refer [campaign-nav]]
            [wish.views.campaign.pages.notes :as notes]))

(def page-headers
  {:notes notes/header})

(defattrs header-style []
  (flex/create
    :flow :horizontal
    :wrap? true
    {:background "#666666"
     :margin-bottom "8px"
     :padding "8px 0"
     :width "100%"})

  [:.nav :.section {:width "50vw"}
   (at-media media/smartphones
     {:width "100%"})
   [:&.full {:width "100% !important"}]])

(defn campaign-header [section]
  (let [page-header (get page-headers section)]
    [:div (header-style)
     [:div.nav (when-not page-header
                 {:class 'full})
      [campaign-nav section]]

     (when page-header
       [:div.section
        [page-header]])]))
