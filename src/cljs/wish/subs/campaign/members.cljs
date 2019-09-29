(ns wish.subs.campaign.members
  (:require [re-frame.core :refer [reg-sub]]
            [wish.subs.campaign.base]))

(reg-sub
  ::candidates
  :<- [:meta/players]
  :<- [:known-sheets]
  (fn [[current-members sheets]]
    (let [by-mine (->> sheets
                       (remove (comp current-members :id))
                       (group-by :mine?))]
      (concat (get by-mine false)
              (get by-mine true)))))

(reg-sub
  ::all
  :<- [:meta/players]
  :<- [:sheets]
  (fn [[char-sheet-ids sheets] _]
    (->> char-sheet-ids
         (map (fn [id]
                (or (assoc (get sheets id)
                           :id id)
                    {:id id})))
         (sort-by :name))))
