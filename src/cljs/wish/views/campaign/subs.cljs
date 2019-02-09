(ns ^{:author "Daniel Leong"
      :doc "Campaign-specific subs"}
  wish.views.campaign.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
  ::add-char-candidates
  :<- [:meta/players]
  :<- [:known-sheets]
  (fn [[current-members sheets]]
    (->> sheets
         (remove (comp current-members :id))
         )))

(reg-sub
  ::campaign-members
  :<- [:meta/players]
  :<- [:sheets]
  (fn [[char-sheet-ids sheets] qv]
    (->> char-sheet-ids
         (map (fn [id]
                (or (assoc (get sheets id)
                           :id id)
                    {:id id})))
         (sort-by :name))))
