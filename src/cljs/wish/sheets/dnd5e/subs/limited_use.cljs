(ns wish.sheets.dnd5e.subs.limited-use
  (:require [re-frame.core :as rf :refer [reg-sub subscribe]]
            [wish.sheets.dnd5e.data :as data]
            [wish.sheets.dnd5e.subs.abilities :as abilities]
            [wish.sheets.dnd5e.subs.inventory :as inventory]
            [wish.sheets.dnd5e.subs.spells :as spells]
            [wish.subs-util :refer [reg-id-sub]]
            [wish.util :refer [invoke-callable]]))

(reg-id-sub
  ::configs
  :<- [:all-limited-use-configs]
  :<- [:total-level]
  :<- [::abilities/modifiers]
  :<- [::inventory/attuned-ids]
  (fn [[items total-level modifiers attuned-set]]
    (->> items
         (remove :implicit?)

         ; eagerly evaluate :uses (the sheet shouldn't do this)
         (map (fn [limited-use]
                (update limited-use
                        :uses
                        (fn [value]
                          (if (ifn? value)
                            (invoke-callable limited-use :uses
                                             :modifiers modifiers
                                             :total-level total-level)
                            value)))))

         ; remove uses that come from un-attuned items that require attunement
         (remove (fn [item]
                   (and (= :item (:wish/context-type item))
                        (:attunes? (:wish/context item))
                        (not (contains? attuned-set (:id (:wish/context item)))))))

         (sort-by :name))))

(reg-sub
  ::by-id
  :<- [::configs]
  :<- [:limited-used]
  (fn [[items used] [_ id]]
    (->> items
         (filter #(= id (:id %)))
         (map #(assoc % :uses-left (- (:uses %)
                                      (get used id))))
         first)))

; Takes an entity with :consumes and returns something that can be
; consumed from it. Usually this delegates to [::by-id (:consumes a)], but
; this also supports the special case of consuming a :*spell-slot
(reg-sub
  ::consumable-for
  (fn [[_ {id :consumes :as entity}]]
    (if (not= :*spell-slot id)
      (subscribe [::by-id id])

      ; special case
      (subscribe [::spells/usable-slot-for entity])))
  (fn [input [_ {id :consumes
                 amount :consumes/amount
                 :as entity}]]
    (if (not= :*spell-slot id)
      ; easy case
      (assoc input
             :consumed-amount
             (cond
               ; easy case:
               (number? amount) amount

               ; also easy:
               (nil? amount) 1

               (fn? amount)
               (let [context (:wish/container entity)
                     result (amount context)]
                 (if (number? result)
                   result

                   (js/console.warn
                     "WARN: " result " returned from :consumes/amount fn for "
                     (:id entity)
                     "\n provided: " (keys context))))

               :else (do
                       (js/console.warn
                         "WARN: unexpected :consumes/amount: " amount)
                       1)))

      {:id :*spell-slot
       :name (str (get data/level-suffixed (:level input))
                  "-level Spell Slot")
       :uses-left (:unused input)
       :slot-kind (:kind input)
       :slot-level (:level input)
       :max-slots (:total input)})))
