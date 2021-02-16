(ns wish.sheets.dnd5e.subs.allies
  (:require [re-frame.core :as rf :refer [reg-sub]]
            [wish-engine.core :as engine]
            [wish.sheets.dnd5e.subs.proficiency :as proficiency]
            [wish.sheets.dnd5e.subs.spells :as spells]
            [wish.sheets.dnd5e.subs.util :refer [filter-by-str]]
            [wish.subs-util :refer [reg-id-sub]]
            [wish.util :refer [invoke-callable]]))

(defn- inflate-actions [engine dice-context ally]
  (concat
    ; attacks first:
    (->> ally
         :attrs
         :attacks
         (map (fn [[id v]]
                (assoc v :id id
                       :from ally
                       :to-hit (apply invoke-callable
                                      v :to-hit
                                      dice-context)
                       :dmg (apply invoke-callable
                              v :dice
                              dice-context)))))

    ; other actions:
    (->> ally
       :attrs
       :actions
       keys
       (keep (fn [id]
               (when-let [f (or (get-in ally [:features id])
                                (get-in engine [:features id]))]
                 (assoc f :from ally
                        :dmg (apply invoke-callable
                                    f :dice
                                    dice-context))))))))
(reg-sub
  ::dice-context
  :<- [::proficiency/bonus]
  :<- [::spells/spell-attack-bonuses]
  (fn [[proficiency-bonus bonuses]]
    {:proficiency-bonus proficiency-bonus
     :spell-bonuses bonuses}))

(reg-sub
  ::with-actions
  :<- [:composite-sheet-engine-state]
  :<- [::dice-context]
  :<- [:allies]
  (fn [[engine dice-context allies]]
    (let [context-list (flatten (seq dice-context))]
      (->> allies
           (transduce
             (map (fn [ally]
                    (assoc ally
                           :actions (inflate-actions
                                      engine
                                      context-list
                                      ally))))
             conj [])))))


; ======= selection/favoriting ============================

(reg-sub
  :5e/allies-filter
  (fn [db]
    (:5e/allies-filter db nil)))

(reg-id-sub
  ::all-inflated
  :<- [:composite-sheet-engine-state]
  :<- [:allies/preferred-map]
  (fn [[source preferred] _]
    (->> (engine/inflate-list source :all-creatures)
         (map (fn [{id :id :as ally}]
                (assoc ally :preferred? (preferred id)))))))

(reg-id-sub
  ::all-sorted
  :<- [::all-inflated]
  (fn [all _]
    (->> all (sort-by :name))))

(reg-sub
  ::all
  :<- [::all-sorted]
  :<- [:5e/allies-filter]
  (fn [[items filter-str]]
    (->> items
         ;; (remove :feature-only?)
         ;; (remove (comp active-ids :id))
         (filter-by-str filter-str))))

