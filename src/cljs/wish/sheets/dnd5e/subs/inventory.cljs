(ns wish.sheets.dnd5e.subs.inventory
  (:require [re-frame.core :as rf :refer [reg-sub]]
            [wish.sheets.dnd5e.data :as data]
            [wish.sheets.dnd5e.subs.util
             :refer [filter-by-str reg-sheet-sub]]))


; ======= items and equipment ==============================

(reg-sheet-sub
  ::currency
  :currency)

(reg-sheet-sub
  ::attuned-ids
  :attuned)

(reg-sub
  ::equipped
  :<- [:equipped-sorted]
  (fn [equipped]
    (map (fn [item]
           (case (:type item)
             :armor (data/inflate-armor item)
             :weapon (data/inflate-weapon item)
             item))
         equipped)))


(reg-sub
  :5e/items-filter
  (fn [db]
    (:5e/items-filter db nil)))

(reg-sub
  ::all-items
  :<- [:all-items]
  :<- [:5e/items-filter]
  (fn [[items filter-str]]
    (filter-by-str filter-str items)))

; all equipped items that are attuned (or that don't need to be attuned)
(reg-sub
  ::attuned
  :<- [::equipped]
  :<- [::attuned-ids]
  (fn [[equipped attuned-set]]
    (->> equipped
         (remove (fn [item]
                   (and (:attunes? item)
                        (not (contains? attuned-set (:id item)))))))))

; returns a map of :kinds and :categories
(reg-sub
  ::eq-proficiencies
  :<- [:classes]
  :<- [:races]
  (fn [entity-lists]
    (->> entity-lists
         flatten
         (map :attrs)
         (reduce
           (fn [m attrs]
             (-> m
                 (update :kinds conj (:weapon-kinds attrs))
                 (update :categories conj (:weapon-categories attrs))))
           {:kinds {}
            :categories {}}))))

; like :inventory-sorted but with :attuned? added as appropriate
(reg-sub
  ::sorted
  :<- [:inventory-sorted]
  :<- [::attuned-ids]
  (fn [[inventory attuned-set]]
    (->> inventory
         (map (fn [item]
                (if (contains? attuned-set (:id item))
                  (assoc item :attuned? true)
                  item))))))

; current quantity of the given item
(reg-sub
  ::item-quantity
  :<- [:inventory-map]
  (fn [m [_ item-id]]
    (->> m item-id :wish/amount)))
