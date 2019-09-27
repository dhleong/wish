(ns ^{:author "Daniel Leong"
      :doc "Campaign workspace"}
  wish.views.campaign.workspace
  (:require [wish.util :refer [<sub #_>evt]]
            #_[wish.util.nav :as nav :refer [sheet-url]]
            #_[wish.views.error-boundary :refer [error-boundary]]
            #_[wish.views.widgets :refer [icon link link>evt]]
            #_[wish.views.campaign.events :as events]
            #_[wish.views.campaign.subs :as subs]))

(defn- id-for [item]
  (or (when (keyword? item)
        item)
      (:id item)))

(defn workspace-item [_entity-card item]
  ; TODO
  [:div (str item)])

(defn workspace [& {:keys [entity-card]}]
  (let [workspace (<sub [:meta/workspace])]
    [:div.workspace
     (for [[item & _secondary] workspace]
       ^{:key (id-for item)}
       [workspace-item entity-card item])]))
