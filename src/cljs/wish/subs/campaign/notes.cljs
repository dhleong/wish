(ns wish.subs.campaign.notes
  (:require [re-frame.core :refer [reg-sub]]
            [wish.subs.campaign.base]))

(defn- inflate-note [n id]
  {:id id
   :modified (:m n)
   :name (:n n)
   :contents (:c n)})

(reg-sub
  ::by-id
  :<- [:meta/notes]
  (fn [notes-map]
    (reduce-kv
      (fn [m id note]
        (assoc m id (inflate-note note id)))
      {}
      notes-map)))

(reg-sub
  ::sorted
  :<- [::by-id]
  (fn [notes-map]
    (->> notes-map
         vals
         (sort-by :modified))))
