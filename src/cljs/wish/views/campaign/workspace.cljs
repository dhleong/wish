(ns ^{:author "Daniel Leong"
      :doc "Campaign workspace"}
  wish.views.campaign.workspace
  (:require [wish.util :refer [<sub >evt]]
            [wish.util.nav :as nav :refer [sheet-url]]
            [wish.views.error-boundary :refer [error-boundary]]
            [wish.views.widgets :refer [icon link link>evt]]
            [wish.views.campaign.events :as events]
            [wish.views.campaign.subs :as subs]))

(defn- id-for [item]
  (or (when (keyword? item)
        item)
      (:id item)))

(defn workspace-item [entity-card item]
  ; TODO
  [:div (str item)])

(defn workspace [& {:keys [entity-card]}]
  (let [workspace (<sub [:meta/workspace])]
    [:div.workspace
     (for [[item & secondary] workspace]
       ^{:key (id-for item)}
       [workspace-item entity-card item])]))
