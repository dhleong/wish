(ns wish.sheets.dnd5e.subs.allies
  (:require [re-frame.core :as rf :refer [reg-sub]]
            ;; [wish-engine.core :as engine]
            [wish.sheets.dnd5e.subs.proficiency :as proficiency]
            [wish.util :refer [distinct-by invoke-callable]]))

(defn- inflate-actions [_engine dice-context ally]
  (println "actions <-" ally)
  (concat
    ; attacks first:
    (->> ally
         :attrs
         :attacks
         (map (fn [[id v]]
                (assoc v :id id
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
  (fn [proficiency-bonus]
    {:proficiency-bonus proficiency-bonus}))

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
