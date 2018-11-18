(ns ^{:author "Daniel Leong"
      :doc "base"}
  wish.views.campaign.base
  (:require [wish.views.campaign.chars-carousel :refer [chars-carousel]]))

(defn campaign-page
  [section & {:keys [char-card]}]
  [:div.campaign
   [chars-carousel char-card]
   "TODO campaign"])
