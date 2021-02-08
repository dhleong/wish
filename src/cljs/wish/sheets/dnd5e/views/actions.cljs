(ns wish.sheets.dnd5e.views.actions
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.util :refer [>evt <sub click>evt]
             :refer-macros [fn-click]]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.overlays :as overlays]
            [wish.sheets.dnd5e.overlays.effects :as effects-manager]
            [wish.sheets.dnd5e.style :as styles]
            [wish.sheets.dnd5e.subs.allies :as allies]
            [wish.sheets.dnd5e.subs.combat :as combat]
            [wish.sheets.dnd5e.subs.limited-use :as limited-use]
            [wish.sheets.dnd5e.subs.spells :as spells]
            [wish.sheets.dnd5e.util :refer [mod->str]]
            [wish.sheets.dnd5e.views.shared :refer [buff-kind-attrs-from-path]]
            [wish.sheets.dnd5e.widgets :as widgets :refer [cast-button]]
            [wish.views.widgets
             :refer-macros [icon]
             :refer [formatted-text link>evt]]))

(defn consume-use-block [consumable opts]
  [widgets/consume-use-block consumable
   (assoc opts :on-click-spell-slots
          (click>evt [:toggle-overlay [#'overlays/info consumable]]))])

; ======= Actions ==========================================

(defn- show-info-for? [s]
  (some s [:aoe :desc :range]))

(defn- attack-block [s & [extra-info]]
  [:div.attack (or extra-info {})
   (when (= :spell-attack (:class extra-info))
     [cast-button s])

   [:div.name
    (when (show-info-for? s)
      {:on-click (click>evt [:toggle-overlay
                             [#'overlays/info s]])
       :class "clickable"})
    [:div.action (:name s)]
    (when-let [source (:from s)]
      [:div.source "From " (:name source)])
    ]

   (when (:consumes s)
     (when-let [{:keys [uses-left] :as info}
                (<sub [::limited-use/consumable-for s])]
       (if (> uses-left 0)
         [:div.uses.button
          {:on-click (click>evt [::events/+use info 1])}
          uses-left " Left"]
         "(none left)\u00A0")))

   (when-let [to-hit (:to-hit s)]
     [:div.info-group
      [:div.label
       "TO HIT"]
      [:div.to-hit
       (mod->str to-hit)]])

   (when-let [save (:save s)]
     [:div.info-group
      [:div.label
       (str/upper-case (name save)) " SAVE"]
      [:div.save-dc
       (:save-dc s)]])

   [:div.info-group
    [:div.label
     "DMG"]
    [:div.dmg (buff-kind-attrs-from-path :dmg (if (:ranged? s)
                                                :ranged
                                                :melee))
     (or (:base-dice s)
         (:dmg s))]
    (when-let [alt-dice (:alt-dice s)]
      [:div.dmg.alt
       "(" alt-dice ")"])]])

(defn- ammunition-block-for [w]
  (if-let [ammo (<sub [::combat/ammunition-for w])]
    [:<>
     (for [a ammo]
       ^{:key (:id a)}
       [:div.ammo {:on-click (click>evt [:toggle-overlay
                                         [#'overlays/info a]])}
        [:div.name (:name a)]
        [:div.amount (:wish/amount a) " Left"]
        (when (> (:wish/amount a) 0)
          [:div.consume.button
           {:on-click (click>evt [:inventory-subtract a]
                                 :propagate? false)}
           "Consume 1"])])]

    [:div.ammo "(no ammunition)"]))

(defn- subscription-attacks [title & {:keys [sub props]}]
  (when-let [attacks (seq (<sub sub))]
    [:div.other
     [:h4 title]
     (for [a attacks]
       ^{:key (:id a)}
       [attack-block a props])]))

(defn- actions-combat []
  [:<>

   [:div.combat-info
    (for [info (<sub [::combat/info])]
      ^{:key (:name info)}
      [:span.item
       (:name info) ": " (:value info)])

    [:a.effects {:href "#"
                 :on-click (click>evt [:toggle-overlay
                                       [#'effects-manager/overlay]])}
     "Add Effect"]]

   (when-let [effects (seq (<sub [:effects]))]
     [:div.effects.combat-info
      "Affected by: "
      (for [effect effects]
        ^{:key (:id effect)}
        [link>evt {:class :item
                   :> [:toggle-overlay [#'overlays/effect-info effect]]}
         (:name effect)])])

   (when-let [s (<sub [::combat/unarmed-strike])]
     [:div.unarmed-strike
      [attack-block (assoc s :name "Unarmed Strike")]])

   (when-let [weapons (seq (<sub [::combat/equipped-weapons]))]
     [:div.weapons
      [:h4 "Weapons"]
      (for [w weapons]
        [:<> {:key (:id w)}
         [attack-block w]
         (when (:uses-ammunition? w)
           [ammunition-block-for w])])])

   [subscription-attacks
    "Spell Attacks"
    :sub [::spells/spell-attacks]
    :props {:class :spell-attack}]
   [subscription-attacks
    "Other Attacks"
    :sub [::combat/other-attacks]]

   (when-let [actions (seq (<sub [::combat/special-actions]))]
     [:div.special
      [:h4 "Special Actions"]
      (for [a actions]
        ^{:key (:id a)}
        [:div.action
         {:on-click (click>evt [:toggle-overlay [#'overlays/info a]])}
         (:name a)])])

   [subscription-attacks
    "Ally Actions"
    :sub [::allies/actions]]
   ])

(defn- action-block [a]
  [:div.action
   [:div.name (:name a)]
   [consume-use-block a]
   [formatted-text :div.desc (:desc a)]])

(defn- actions-for-type [filter-type header-form]
  (let [spells (seq (<sub [::spells/prepared-spells-filtered filter-type]))
        actions (seq (<sub [::combat/actions-for-type filter-type]))]
    (when (or spells actions)
      [:<> {:key filter-type}
       header-form

       (when spells
         [:div.spells
          [:div.section-label "Spells"]

          (for [s spells]
            ^{:key [(::spells/source s) (:id s)]}
            [:div.spell-name.clickable
             {:on-click (click>evt [:toggle-overlay [#'overlays/spell-info s]])
              :class [(when (:no-slot? s)
                        :no-slot)
                      (when (:requires-upcast? s)
                        :requires-upcast)]}
             (:name s)])])

       (when actions
         [:div.actions
          (for [a actions]
            ^{:key (:id a)}
            [action-block a])])])))

(defn- scroll-into-view [el]
  (.scrollIntoView el #js {:behavior "smooth"
                           :block "start"
                           :inline "center"}))

(defn- combat-page-link
  [state id label selected?]
  [:div.filter {:class (when selected?
                         "selected")}
   (if selected?
     [:span.unselectable label]

     [:a {:href "#"
          :on-click (fn-click
                      (let [el (get-in @state [:elements id])]
                        (scroll-into-view el)))}
      label])])

(def ^:private action-pages
  [[:combat "Combat"]
   [:actions "Actions" :when-any-<sub [[::spells/prepared-spells-filtered :action]
                                       [::combat/actions-for-type :action]]]
   [:bonuses "Bonuses" :when-any-<sub [[::spells/prepared-spells-filtered :bonus]
                                       [::combat/actions-for-type :bonus]]]
   [:reactions "Reactions" :when-any-<sub [[::spells/prepared-spells-filtered :reaction]
                                           [::combat/actions-for-type :reaction]]]
   [:specials "Others" :when-any-<sub [[::spells/prepared-spells-filtered :special-action]
                                       [::combat/actions-for-type :special-action]]]
   [:limited-use "Limited" :when-any-<sub [[::limited-use/configs]]]])

(defn- page->index [pages to-find]
  (reduce-kv
    (fn [_ index [page-id _]]
      (when (= to-find page-id)
        (reduced index)))
    nil
    pages))

(defn- window-of [pages around-id]
  (let [page-index (page->index pages around-id)
        max-index (dec (count pages))

        before (- page-index 2)
        before-delta (when (< before 0)
                       (- before))

        after (+ page-index 2)
        after-delta (when (> after max-index)
                      (- max-index after))

        start (max 0 (+ before
                        after-delta))
        end (inc (min max-index
                      (+ after
                         before-delta)))]
    (subvec pages start end)))

(defn- actions-header [state header-id]
  (let [smartphone? (= :smartphone (<sub [:device-type]))
        available-pages (->> action-pages
                             (filter (fn [[_ _ & {:keys [when-any-<sub]}]]
                                       (or (nil? when-any-<sub)
                                           (some (comp seq <sub) when-any-<sub))))
                             vec)
        pages-to-show (if smartphone?
                        ; show subset, for space
                        (window-of available-pages header-id)

                        ; all pages
                        available-pages)]
    [:div.filters {:ref #(swap! state assoc-in [:elements header-id] %)}
     (when (not= (ffirst pages-to-show) (ffirst action-pages))
       [combat-page-link state (ffirst action-pages) "…" false])

     (for [[id label] pages-to-show]
       (let [selected? (= id header-id)]
         ^{:key id}
         [combat-page-link state id label selected?]))

     (when (not= (first (peek pages-to-show)) (first (peek action-pages)))
       [combat-page-link state (first (peek action-pages)) "…" false])
     ]))

; ======= Limited-use ======================================

(defn usage-box-single
  "Render a toggle button for whether a single-use item has been used"
  [item used?]
  [:div.button
   {:class (when used?
             "selected")
    :on-click (click>evt [::events/toggle-used item])}
   (if used?
     "Used"
     "Use")])

(defn usage-box-many
  "Render a box for specifying how many uses remain"
  [item uses-left uses-max]
  [:div.many
   [:a.modify {:href "#"
               :class (when (<= uses-left 0)
                        "disabled")
               :on-click (click>evt [::events/+use item])}
    (icon :remove-circle)]

   [bind-fields
    [:input.uses-left {:field :fast-numeric
                       :id :uses}]

    {:get #(:uses-left (<sub [::limited-use/by-id (:id item)]))
     :save! (fn [_ v]
              (let [used (max 0 (min uses-max
                                     (- uses-max v)))]
                (>evt [:set-used! (:id item) used])))}]

   [:a.modify {:href "#"
               :class (when (>= uses-left uses-max)
                        "disabled")
               :on-click (click>evt [:+use (:id item) -1])}
    (icon :add-circle)]])

(defn usage-box
  "Render some sort of box for 'using' a limited-use item,
   appropriate to the number of total possible `uses` and
   indicating the current `used-count`."
  [item uses used-count]
  [:div.usage
   (cond
     (= 1 uses) [usage-box-single item (> used-count 0)]
     :else [usage-box-many item (- uses used-count) uses])])


(def trigger-labels
  {:short-rest "Short Rest"
   :long-rest "Long Rest"})

(defn describe-uses
  [item uses trigger]
  (cond
    (:restore-desc item) (:restore-desc item)
    (= 1 uses) (str "Once per " (trigger-labels trigger))
    :else (str uses " uses / " (trigger-labels trigger))))

(defn limited-use-section []
  (let [items (<sub [::limited-use/configs])
        used (<sub [:limited-used])]
    [:div (styles/limited-use-section)
     (if-not (empty? items)
       (for [item items]
         (let [uses (:uses item)
               used-count (get used (:id item))]
           ^{:key (:id item)}
           [:div.limited-use
            [:div.info
             [:div.name (:name item)]
             [:div.recovery
              (describe-uses item uses (:restore-trigger item))]]
            [usage-box item uses used-count]]))

       [:div.explanation
        [:p "Features, traits, and abilities that can only be used a limited number of times before requiring a rest will be listed here."]
        [:p "You will gain limited-use actions as you adventure and level up."]])
     ]))


; ======= main section ====================================

(defn view []
  (r/with-let [page-state (r/atom nil)]
    [:<>

     [actions-header page-state :combat]
     [actions-combat]

     [actions-for-type :action
      [actions-header page-state :actions]]

     [actions-for-type :bonus
      [actions-header page-state :bonuses]]

     [actions-for-type :reaction
      [actions-header page-state :reactions]]

     [actions-for-type :special-action
      [actions-header page-state :specials]]

     [actions-header page-state :limited-use]
     [limited-use-section]]))

