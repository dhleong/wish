(ns wish.views.campaign.pages.home
  (:require [wish.views.campaign.chars-carousel :refer [chars-carousel]]
            [wish.views.campaign.workspace :refer [workspace]]))

(defn page [{:keys [char-card entity-card]}]
  [:<>
    [chars-carousel char-card]

    [workspace
     :entity-card entity-card]])
