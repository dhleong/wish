(ns ^{:author "Daniel Leong"
      :doc "Campaign-specific subs"}
  wish.views.campaign.subs
  (:require [re-frame.core :refer [reg-sub]]))

(defn- reg-meta-sub
  [id getter]
  (reg-sub
    id
    :<- [:sheet-meta]
    (fn [sheet _]
      (getter sheet))))

(reg-meta-sub :meta/workspace :workspace)

;;
;; Campaign members
;;

(reg-sub
  ::add-char-candidates
  :<- [:meta/players]
  :<- [:known-sheets]
  (fn [[current-members sheets]]
    (let [by-mine (->> sheets
                       (remove (comp current-members :id))
                       (group-by :mine?))]
      (concat (get by-mine false)
              (get by-mine true)))))

(reg-sub
  ::campaign-members
  :<- [:meta/players]
  :<- [:sheets]
  (fn [[char-sheet-ids sheets] _]
    (->> char-sheet-ids
         (map (fn [id]
                (or (assoc (get sheets id)
                           :id id)
                    {:id id})))
         (sort-by :name))))
