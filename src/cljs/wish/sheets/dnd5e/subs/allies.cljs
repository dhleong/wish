(ns wish.sheets.dnd5e.subs.allies
  (:require [re-frame.core :as rf :refer [reg-sub]]
            ;; [wish-engine.core :as engine]
            [wish.sheets.dnd5e.subs.proficiency :as proficiency]
            [wish.sheets.dnd5e.subs.spells :as spells]
            [wish.util :refer [distinct-by invoke-callable]]))

(defn- inflate-actions [_engine dice-context ally]
  (concat
    ; attacks first:
    (->> ally
         :attrs
         :attacks
         (map (fn [[id v]]
                (assoc v :id id
                       :from ally
                       :to-hit (when (:to-hit v)
                                 (apply invoke-callable
                                        v :to-hit
                                        dice-context))
                       :dmg (apply invoke-callable
                              v :dice
                              dice-context)))))

    ; TODO other actions:
    #_(->> ally
       :attrs
       :actions
       keys)
    )
  )

(reg-sub
  ::dice-context
  :<- [::proficiency/bonus]
  :<- [::spells/spell-attack-bonuses]
  (fn [[proficiency-bonus bonuses]]
    {:proficiency-bonus proficiency-bonus
     :spell-bonuses bonuses}))

(reg-sub
  ::actions
  :<- [:composite-sheet-engine-state]
  :<- [::dice-context]
  :<- [:allies]
  (fn [[engine dice-context allies]]
    (->> allies
         (transduce
           (comp
             (distinct-by :id)
             (mapcat (partial inflate-actions
                              engine
                              (flatten (seq dice-context)))))
           conj []))))
