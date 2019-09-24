(ns ^{:author "Daniel Leong"
      :doc "DND 5e sheet"}
  wish.sheets.dnd5e
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [wish.util :refer [>evt <sub click>evt]]
            [wish.util.nav :refer [sheet-url]]
            [wish.util.scroll :refer [scrolled-amount]]
            [wish.sheets.dnd5e.overlays :as overlays]
            [wish.sheets.dnd5e.style :as styles]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.util :refer [mod->str]]
            [wish.sheets.dnd5e.views.abilities :as abilities]
            [wish.sheets.dnd5e.views.actions :as actions]
            [wish.sheets.dnd5e.views.features :as features]
            [wish.sheets.dnd5e.views.inventory :as inventory]
            [wish.sheets.dnd5e.views.shared :refer [buff-kind->attrs]]
            [wish.sheets.dnd5e.views.spells :as spells]
            [wish.views.error-boundary :refer [error-boundary]]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
             :refer [link link>evt]]
            [wish.views.widgets.swipeable :refer [swipeable]]))

(def ^:private nav-ref (atom nil))

; ======= Top bar ==========================================

(defn- hp-normal [hp max-hp]
  (let [buff-kind (<sub [::subs/effect-change-for :hp-max])]
    [:<>
     [:div.label [:span.content "Hit Points"]]
     [:div.value
      [:div.now hp]
      [:div.divider " / "]
      [:div.max (buff-kind->attrs buff-kind)
       max-hp]]]))

(defn- save-indicators
  [prefix icon-class used]
  [:div.indicators
   prefix

   (for [i (range 3)]
     (with-meta
       (if (< i used)
         (icon :radio-button-checked.icon {:class icon-class})
         (icon :radio-button-unchecked.icon {:class icon-class}))
       {:key i}))])

(defn hp-death-saving-throws
  ([] (hp-death-saving-throws nil))
  ([sheet-id]
   (let [{:keys [saves fails]} (<sub [::subs/death-saving-throws sheet-id])]
     [:<>
      [save-indicators "😇" :save saves]
      [save-indicators "☠️" :fail fails]])))

(defn hp []
  (let [[hp max-hp] (<sub [::subs/hp])]
    [:div.clickable.hp.col
     {:on-click (click>evt [:toggle-overlay [#'overlays/hp-overlay]])}

     (if (> hp 0)
       [hp-normal hp max-hp]
       [hp-death-saving-throws])]))

(defn buffable-stat [stat-id label & content]
  (let [buff-kind (<sub [::subs/effect-change-for stat-id])]
    [:div.col
     (into [:div.stat (buff-kind->attrs buff-kind)]
           content)
     [:div.label label]]))

(defn header []
  (let [common (<sub [:sheet-meta])
        classes (<sub [:classes])]
    [:div styles/header-container
     [:div styles/header
      [:div.left.side
       [:div.col
        [widgets/save-state]]

       [:div.col.left.meta
        [:div.name [link {:class "inline"
                          :href "/sheets"}
                    (:name common)]]
        [:div.meta
         [:div.race (:name (<sub [:race]))]
         [:div.classes (->> classes
                            (map (fn [c]
                                   (str (-> c :name) "\u00a0" (:level c))))
                            (str/join " / "))]]]

       [:div.col
        [link>evt [:toggle-overlay [#'overlays/notes-overlay]]
         (icon :description)]]]

      [:div.space]

      [:div.share.side
       [:div.col
        [widgets/share-button]]]

      [:div.settings.side
       [:div.col
        (let [sheet-id (<sub [:active-sheet-id])]
          [link {:href (sheet-url sheet-id :builder :class)}
           (icon :settings)])]]

      [:div.right.side
       [:div.col
        [:div.stat (mod->str
                     (<sub [::subs/proficiency-bonus]))]
        [:div.label "Proficiency"]]

       [buffable-stat :ac "AC"
        (<sub [::subs/ac])]

       [buffable-stat :speed "Speed"
        (<sub [::subs/speed]) [:span.unit " ft"]]

       [:div.col
        [:div.stat (<sub [::subs/passive-perception])]
        [:div.label "Pass. Perc."]]

       [buffable-stat :initiative "Initiative"
        (mod->str
          (<sub [::subs/initiative]))]

       [hp]]
      ]]))





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
  (let [spellcasters (seq (<sub [::subs/spellcaster-blocks]))
        smartphone? (= :smartphone (<sub [:device-type]))
        page (<sub [::subs/page])]
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

      [swipeable {:get-key #(<sub [::subs/page])
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
    [header]]

   [:div styles/layout
    (when-not (= :smartphone (<sub [:device-type]))
      [error-boundary
       [:div.left.side
        [abilities/view]]])

    [:div.right.side
     [sheet-right-page]]]])
