(ns wish.subs.campaign.notes
  (:require [re-frame.core :refer [reg-sub]]
            [wish.subs.campaign.base]))

(reg-sub
  ::by-id
  :<- [:meta/notes]
  (fn [notes-map]
    (reduce-kv
      (fn [m id note]
        ; TODO inflate
        (assoc m id note))
      {}
      notes-map)))
