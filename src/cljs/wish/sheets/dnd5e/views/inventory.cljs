(ns wish.sheets.dnd5e.views.inventory
  (:require [wish.inventory :as inv]
            [wish.util :refer [<sub click>evt]]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.overlays :as overlays]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.util :refer [equippable?]]
            [wish.sheets.dnd5e.widgets :refer [item-quantity-manager
                                               currency-preview]]
            [wish.views.widgets
             :refer-macros [icon]
             :refer [expandable formatted-text link>evt]]))

; ======= inventory ========================================

(defn- inventory-entry
  [item can-attune?]
  (let [{:keys [type]
         quantity :wish/amount} item
        stacks? (inv/stacks? item)]
    [expandable
     [:div.item {:class [(when (:wish/equipped? item)
                           "equipped")
                         (when (:attuned? item)
                           "attuned")]}
      [:div.info
       [:div.name (:name item)]
       (when-let [n (:notes item)]
         [:div.notes-preview n])]

      (when (inv/custom? item)
        [:div.edit
         [link>evt {:> [:toggle-overlay
                        [#'overlays/custom-item-overlay item]]
                    :propagate? false}
          (icon :settings)]])

      (when (inv/instanced? item)
        [:div.notes
         [link>evt {:> [:toggle-overlay
                        [#'overlays/notes-overlay :item item]]
                    :propagate? false}
          (icon :description)]])

      (when stacks?
        [:div.quantity quantity])

      (when (= :ammunition type)
        [:div.consume.button
         {:on-click (click>evt [:inventory-subtract item]
                               :propagate? false)}
         "Consume 1"])

      (when (equippable? item)
        [:div.equip.button
         {:on-click (click>evt [:toggle-equipped item]
                               :propagate? false)}
         (if (:wish/equipped? item)
           "Unequip"
           "Equip")])

      (when (:attunes? item)
        [:div.attune.button
         {:class (when-not (or (:attuned? item)
                               can-attune?)
                   ; "limit" 3 attuned
                   "disabled")

          :on-click (click>evt [::events/toggle-attuned item]
                               :propagate? false)}

         (if (:attuned? item)
           "Unattune"
           "Attune")])]

     [:div.item-info
      [formatted-text :div.desc (:desc item)]

      (when stacks?
        [item-quantity-manager item])

      [:a.delete {:href "#"
           :on-click (click>evt [:inventory-delete item])}
       (icon :delete-forever)
       " Delete" (when stacks? " all") " from inventory"]]]) )


; ======= public interface ================================

(defn view []
  [:<>
   [:span.clickable
    {:class "clickable"
     :on-click (click>evt [:toggle-overlay
                           [#'overlays/currency-manager]])}
    [currency-preview :large]]

   [:div.add
    [:b.label "Add:"]
    [link>evt {:class "link"
               :> [:toggle-overlay
                   [#'overlays/item-adder]]}
     "Item"]

    [link>evt {:class "link"
               :> [:toggle-overlay
                   [#'overlays/custom-item-overlay]]}
     "Custom"]

    [link>evt {:class "link"
               :> [:toggle-overlay
                   [#'overlays/starting-equipment-adder]]}
     "Starting Gear"] ]

   (when-let [inventory (seq (<sub [::subs/inventory-sorted]))]
     (let [can-attune? (< (count (<sub [::subs/attuned-ids]))
                          3)]
       (for [item inventory]
         ^{:key (:id item)}
         [inventory-entry item can-attune?])))
   ])

