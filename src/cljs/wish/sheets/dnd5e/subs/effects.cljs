(ns wish.sheets.dnd5e.subs.effects
  (:require [re-frame.core :as rf :refer [reg-sub]]
            [wish.sheets.dnd5e.subs.abilities :as abilities]
            [wish.sheets.dnd5e.subs.util
             :refer [compute-buffs filter-by-str]]))

; ======= effects =========================================

(reg-sub
  :5e/effects-filter
  (fn [db]
    (:5e/effects-filter db nil)))

; returns a map of id -> {buff-id -> n}
(reg-sub
  ::buffs-map
  :<- [:effects]
  (fn [effects _]
    (->> effects
         (map (comp :buffs :attrs))
         (apply merge-with merge))))

(reg-sub
  ::buffs-values-map
  :<- [::buffs-map]
  :<- [::abilities/modifiers]
  :<- [:wish.sheets.dnd5e.subs/base-speed]
  (fn [[effects modifiers speed] _]
    ; NOTE: for now it is okay that we don't necessarily get
    ; the complete buff value for speed...
    (let [entity {:modifiers modifiers
                  :speed speed}]
      (reduce-kv
        (fn [m effect-id buffs-map]
          (assoc m effect-id
                 (compute-buffs
                   entity
                   buffs-map)))
        {}
        effects))))

; :buff, :nerf, or nil for the given ID
(reg-sub
  ::change-for
  :<- [::buffs-values-map]
  (fn [buffs-map [_ id]]
    (when-let [value (get buffs-map id)]
      (cond
        (> value 0) :buff
        (< value 0) :nerf
        :else nil))))

(reg-sub
  ::all
  :<- [:all-effects/sorted]
  :<- [:effect-ids-set]
  :<- [:5e/effects-filter]
  (fn [[items active-ids filter-str]]
    (->> items
         (remove :feature-only?)
         (remove (comp active-ids :id))
         (filter-by-str filter-str))))
