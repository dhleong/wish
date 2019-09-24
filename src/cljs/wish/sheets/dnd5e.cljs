(ns ^{:author "Daniel Leong"
      :doc "DND 5e sheet"}
  wish.sheets.dnd5e
  (:require [reagent.core :as r]
            [wish.util :refer [>evt <sub click>evt]]
            [wish.util.scroll :refer [scrolled-amount]]
            [wish.sheets.dnd5e.style :as styles]
            [wish.sheets.dnd5e.subs.spells :as subs-spells]
            [wish.sheets.dnd5e.subs.nav :as subs-nav]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.views.abilities :as abilities]
            [wish.sheets.dnd5e.views.actions :as actions]
            [wish.sheets.dnd5e.views.features :as features]
            [wish.sheets.dnd5e.views.header :as header]
            [wish.sheets.dnd5e.views.inventory :as inventory]
            [wish.sheets.dnd5e.views.spells :as spells]
            [wish.views.error-boundary :refer [error-boundary]]
            [wish.views.widgets.swipeable :refer [swipeable]]))

(def ^:private nav-ref (atom nil))

; ======= Main interface ===================================

(defn- nav-link
  [page id label]
  (let [selected? (= id page)]
    [:h1.section
     {:class (when selected?
               "selected")
      :on-click (click>evt [::events/page! id])}
     label]))

(defn- main-section
  [{id :key} page opts content]
  ; NOTE: NOT a ratom, else we get an endless render loop
  (r/with-let [view-ref (atom nil)]
    (let [selected? (= id page)
          r @view-ref
          nav @nav-ref]
      (when (and selected? r nav)
        ; if we've scrolled past the nav bar, ensure this view is visible
        ; (if we're at the top, it is annoying and doesn't matter anyway)
        (when (>= (scrolled-amount r)
                  (.-offsetTop nav))
            (.scrollIntoView r #js {:behavior "smooth"
                                    :block "nearest"
                                    :inline "nearest"}))))
    [:div.section (assoc opts :ref #(reset! view-ref %))
     content]))

(defn- sheet-right-page []
  (let [spellcasters (seq (<sub [::subs-spells/spellcaster-blocks]))
        smartphone? (= :smartphone (<sub [:device-type]))
        page (<sub [::subs-nav/page])]
    [:<>
     [:div.nav {:ref #(reset! nav-ref %)}
      (when smartphone?
        [nav-link page :abilities "Abilities"])
      [nav-link page :actions "Actions"]
      (when spellcasters
        [nav-link page :spells "Spells"])
      [nav-link page :inventory "Inventory"]
      [nav-link page :features "Features"]]

     ; actual sections
     [error-boundary

      [swipeable {:get-key #(<sub [::subs-nav/page])
                  :set-key! #(>evt [::events/page! %])}

       (when smartphone?
         [main-section {:key :abilities} page
          nil
          [abilities/view]])

       [main-section {:key :actions} page
        styles/actions-section
        [actions/view]]

       (when spellcasters
         [main-section {:key :spells} page
          styles/spells-section
          [spells/view spellcasters]])

       [main-section {:key :inventory} page
        styles/inventory-section
        [inventory/view]]

       [main-section {:key :features} page
        styles/features-section
        [features/view]]

       ]] ]))

(defn sheet []
  [:div styles/container
   [error-boundary
    [header/view]]

   [:div styles/layout
    (when-not (= :smartphone (<sub [:device-type]))
      [error-boundary
       [:div.left.side
        [abilities/view]]])

    [:div.right.side
     [sheet-right-page]]]])
