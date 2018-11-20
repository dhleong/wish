(ns ^{:author "Daniel Leong"
      :doc "base"}
  wish.views.campaign.base
  (:require [wish.views.campaign.chars-carousel :refer [chars-carousel]]
            [wish.views.error-boundary :refer [error-boundary]]))

(defn campaign-page
  [section & {:keys [char-card]}]
  [error-boundary
   [:div.campaign
    [chars-carousel char-card]
    "TODO campaign"]])
