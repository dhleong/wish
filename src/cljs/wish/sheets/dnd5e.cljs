(ns ^{:author "Daniel Leong"
      :doc "DND 5e sheet"}
  wish.sheets.dnd5e
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [wish.util :refer [>evt <sub click>evt
                               invoke-callable]]
            [wish.util.nav :refer [sheet-url]]
            [wish.util.scroll :refer [scrolled-amount]]
            [wish.sheets.dnd5e.overlays :as overlays]
            [wish.sheets.dnd5e.style :as styles]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.util :refer [mod->str]]
            [wish.sheets.dnd5e.views.abilities :as abilities]
            [wish.sheets.dnd5e.views.actions :as actions]
            [wish.sheets.dnd5e.views.inventory :as inventory]
            [wish.sheets.dnd5e.views.shared :refer [buff-kind->attrs]]
            [wish.sheets.dnd5e.widgets :refer [cast-button
                                               spell-card
                                               spell-tags]]
            [wish.views.error-boundary :refer [error-boundary]]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
             :refer [expandable formatted-text link link>evt]]
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




; ======= Features =========================================

(defn feature [f]
  (let [values (seq (:values f))]
    [:div.feature
     [:div.name (:name f)]

     [actions/consume-use-block f {:omit-name (:name f)}]

     [formatted-text :div.desc (:desc f)]

     (when values
       [:div.chosen-details
        [:h5 "Chosen values:"]
        (for [v values]
          ^{:key (:id v)}
          [:div.chosen.clickable
           {:on-click (click>evt [:toggle-overlay
                                  [#'overlays/info v]])}
           (:name v)])])]))

(defn features-section []
  [:<>
   (when-let [fs (<sub [::subs/features-for [:inflated-class-features]])]
      [:div.features-category
       [:h3 "Class features"]
       (for [f fs]
         ^{:key (:id f)}
         [feature f])])

    (when-let [fs (<sub [::subs/features-for [:inflated-race-features]])]
      [:div.features-category
       [:h3 "Racial Traits"]
       (for [f fs]
         ^{:key (:id f)}
         [feature f])])

    ; TODO proficiencies?
    ; TODO feats?
    ])



; ======= Spells ===========================================

(defn spell-block
  [s]
  (let [base-level (:spell-level s)
        cantrip? (= 0 base-level)
        {cast-level :level} (<sub [::subs/usable-slot-for s])
        upcast? (when cast-level
                  (not= cast-level base-level))
        level (or cast-level base-level)]
    [expandable
     [:div.spell
      [cast-button {:nested? true} s]

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
     [spell-card s]]))

(defn spell-slot-use-block
  [kind level total used]
  [widgets/slot-use-block
   {:total total
    :used used
    :consume-evt [::events/use-spell-slot kind level total]
    :restore-evt [::events/restore-spell-slot kind level total]}])

(defn spells-list [spells]
  [:<>
   (for [s spells]
     ^{:key (:id s)}
     [spell-block s])])

(defn- spellcaster-info [spellcaster]
  (let [info (<sub [::subs/spellcaster-info (:id spellcaster)])]
    [:span.spellcaster-info
     [:span.item "Modifier: " (mod->str (:mod info))]
     [:span.item "Attack: " (mod->str (:attack info))]
     [:span.item "Save DC: " (:save-dc info)]
     ]))

(defn spells-section [spellcasters]
  (let [slots-sets (<sub [::subs/spell-slots])
        slots-used (<sub [::subs/spell-slots-used])
        prepared-spells-by-class (<sub [::subs/prepared-spells-by-class])]
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
                         [#'overlays/spell-management s]
                         :scrollable? true]
               (str "Manage " prepared-label " spells")]])
           (when acquires?
             [:div.manage-link
              [link>evt [:toggle-overlay
                         [#'overlays/spell-management
                          s
                          :mode :acquisition]
                         :scrollable? true]
               (str "Manage " (:acquired-label s))]])]

          (when-not fixed-list?
            [:div.list-info (str (str/capitalize prepared-label) " Spells")
             [:span.count "(" (count prepared-spells) ")"]])

          (if any-prepared?
            [spells-list prepared-spells]
            [:div (str "You don't have any " prepared-label " spells")])]))]))



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
          [spells-section spellcasters]])

       [main-section {:key :inventory} page
        styles/inventory-section
        [inventory/view]]

       [main-section {:key :features} page
        styles/features-section
        [features-section]]

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
