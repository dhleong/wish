(ns wish.sheets.dnd5e.views.spells
  (:require [clojure.string :as str]
            [wish.util :refer [<sub invoke-callable]]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.overlays.spell-management
             :as spell-management]
            [wish.sheets.dnd5e.subs.spells :as spells]
            [wish.sheets.dnd5e.util :refer [mod->str]]
            [wish.sheets.dnd5e.widgets :refer [cast-button
                                               spell-card
                                               spell-tags]]
            [wish.views.widgets :as widgets
             :refer [expandable link>evt]]))

; ======= Spells ===========================================

(defn spell-block [s]
  (let [base-level (:spell-level s)
        cantrip? (= 0 base-level)
        {cast-level :level} (<sub [::spells/usable-slot-for s])
        upcast? (when cast-level
                  (not= cast-level base-level))
        level (or cast-level base-level)]
    [:<>
     [cast-button {:nested? true} s]
     [expandable
      [:div.spell
       [cast-button {:placeholder? true} s]

       [:div.spell-info
        [:div.name (:name s)]

        [:div.meta {:class (when upcast?
                             "upcast")}
         (if cantrip?
           "Cantrip"
           (str "Level " level))
         ; concentration? ritual?
         [spell-tags s]]]

       (cond
         (:dice s)
         [:div.dice {:class (when upcast?
                              "upcast")}
          (invoke-callable
            (assoc s :spell-level level)
            :dice)
          (when-let [buffs (:buffs s)]
            (when-let [buff (buffs s)]
              (str " + " buff)))
          ]

         (:save s)
         [:div.dice
          [:div.meta (:save-label s)]
          (:save-dc s)]
         )]

      ; collapsed:
      [spell-card s]]]))

(defn spell-slot-use-block
  [kind level total used]
  [widgets/slot-use-block
   {:total total
    :used used
    :consume-evt [::events/use-spell-slot kind level total]
    :restore-evt [::events/restore-spell-slot kind level total]}])

(defn- spells-list [spells]
  [:<>
   (for [s spells]
     ^{:key (:id s)}
     [spell-block s])])

(defn- spellcaster-info [spellcaster]
  (let [info (<sub [::spells/spellcaster-info (:id spellcaster)])]
    [:span.spellcaster-info
     [:span.item "Modifier: " (mod->str (:mod info))]
     [:span.item "Attack: " (mod->str (:attack info))]
     [:span.item "Save DC: " (:save-dc info)]
     ]))

(defn view [spellcasters]
  (let [slots-sets (<sub [::spells/spell-slots])
        slots-used (<sub [::spells/spell-slots-used])
        prepared-spells-by-class (<sub [::spells/prepared-spells-by-class])]
    [:<>
     (for [[id {:keys [label slots]}] slots-sets]
       ^{:key id}
       [:div.spell-slots
        [:h4 label]
        (for [[level total] slots]
          ^{:key (str "slots/" level)}
          [:div.spell-slot-level
           [:div.label
            (str "Level " level)]
           [spell-slot-use-block
            id level total (get-in slots-used [id level])]])])

     (for [s spellcasters]
       (let [prepared-spells (get prepared-spells-by-class (:id s))
             prepares? (:prepares? s)
             acquires? (:acquires? s)
             fixed-list? (not (:spells s))
             any-prepared? (> (count prepared-spells) 0)
             prepared-label (if prepares?
                              "prepared"
                              "known")]
         ^{:key (:id s)}
         [:div.spells
          [:h4 (:name s)

           [spellcaster-info s]

           (when-not (or fixed-list?
                         (and acquires?
                              (not prepares?)))
             [:div.manage-link
              [link>evt [:toggle-overlay
                         [#'spell-management/overlay s]]
               (str "Manage " prepared-label " spells")]])
           (when acquires?
             [:div.manage-link
              [link>evt [:toggle-overlay
                         [#'spell-management/overlay
                          s
                          :mode :acquisition]]
               (str "Manage " (:acquired-label s))]])]

          (when-not fixed-list?
            [:div.list-info (str (str/capitalize prepared-label) " Spells")
             [:span.count "(" (count prepared-spells) ")"]])

          (if any-prepared?
            [spells-list prepared-spells]
            [:div (str "You don't have any " prepared-label " spells")])]))]))



