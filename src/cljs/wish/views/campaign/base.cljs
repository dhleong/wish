(ns wish.views.campaign.base
  (:require [wish.views.campaign.pages.home :as home]
            [wish.views.campaign.pages.header :refer [campaign-header]]
            [wish.views.campaign.pages.notes :as notes]
            [wish.views.campaign.pages.spaces :as spaces]
            [wish.views.error-boundary :refer [error-boundary]]))

; ======= public interface ================================

(defn campaign-page [section & {:as opts}]
  [:div.campaign
   [campaign-header section]

   [error-boundary
    (case section
      nil [home/page opts]
      :notes [notes/page]
      :spaces [spaces/page])]])
