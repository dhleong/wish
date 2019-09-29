(ns ^{:author "Daniel Leong"
      :doc "Campaign workspace"}
  wish.views.campaign.workspace
  (:require [wish.util :refer [<sub]]
            [wish.subs.campaign.workspace :as workspace]))

(defn- id-for [item]
  (or (when (keyword? item)
        item)
      (:id item)))

(defn- space [_entity-card item]
  ; TODO
  [:div (str item)])


; ======= public interface ================================

(defn workspace [& {:keys [entity-card]}]
  (let [spaces (<sub [::workspace/spaces])]
    [:div.workspace
     (for [s spaces]
       ^{:key (id-for s)}
       [space entity-card s])]))
