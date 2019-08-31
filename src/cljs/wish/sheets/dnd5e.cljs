(ns ^{:author "Daniel Leong"
      :doc "DND 5e sheet"}
  wish.sheets.dnd5e
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.util :refer [>evt <sub click>evt
                               invoke-callable]]
            [wish.util.nav :refer [sheet-url]]
            [wish.inventory :as inv]
            [wish.sheets.dnd5e.overlays :as overlays]
            [wish.sheets.dnd5e.overlays.effects :as effects-manager]
            [wish.sheets.dnd5e.style :as styles]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.util :refer [equippable? mod->str]]
            [wish.sheets.dnd5e.widgets :refer [item-quantity-manager
                                               cast-button
                                               currency-preview
                                               spell-card
                                               spell-tags]]
            [wish.views.error-boundary :refer [error-boundary]]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
             :refer [expandable formatted-text link link>evt]]
            [wish.views.widgets.swipeable :refer [swipeable]]))

(defn rest-buttons []
  [:div styles/rest-buttons
   [:div.button.short
    {:on-click (click>evt [:toggle-overlay [#'overlays/short-rest-overlay]])}
    "Short Rest"]
   [:div.button.long
    {:on-click (click>evt [:toggle-overlay [#'overlays/long-rest-overlay]])}
    "Long Rest"]])


; ======= Top bar ==========================================

(defn- hp-normal [hp max-hp]
  [:<>
   [:div.label [:span.content "Hit Points"]]
   [:div.value
    [:div.now hp]
    [:div.divider " / "]
    [:div.max max-hp]]])

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
     (into [:div.stat (when buff-kind
                        {:class (str (name buff-kind) "ed")})]
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


; ======= abilities ========================================

;; (def labeled-abilities
;;   [[:str "Strength"]
;;    [:dex "Dexterity"]
;;    [:con "Constitution"]
;;    [:int "Intelligence"]
;;    [:wis "Wisdom"]
;;    [:cha "Charisma"]])

(def labeled-abilities
  [[:str "STR"]
   [:dex "DEX"]
   [:con "CON"]
   [:int "INT"]
   [:wis "WIS"]
   [:cha "CHA"]])

(defn abilities-display
  ([abilities] (abilities-display abilities false))
  ([abilities clickable?]
   [:<>
    (for [[id label] labeled-abilities]
      (let [{:keys [score modifier mod]} (get abilities id)]
        ^{:key id}
        [:div.ability {:class (when mod
                                (case mod
                                  :buff "buffed"
                                  :nerf "nerfed"))
                       :on-click (when clickable?
                                   (click>evt [:toggle-overlay
                                               [#'overlays/ability-tmp
                                                id
                                                label]]))}
         [:div.label label]
         [:div.mod modifier]
         [:div.score "(" score ")"]
         ]))]))

(defn abilities-section []
  (let [abilities (<sub [::subs/ability-info])
        save-extras (<sub [::subs/ability-extras])]
    [:div styles/abilities-section
     [:div.abilities
      [abilities-display abilities :clickable]]

     [:div.info "Saves"]

     [:div.abilities
      (for [[id _label] labeled-abilities]
        (let [{:keys [save proficient?]} (get abilities id)]
          ^{:key id}
          [:span.save
           [:div.mod save]
           [:div.proficiency
            {:class (when proficient?
                      "proficient")}]]))]

     ; This is a good place for things like Elven advantage
     ; on saving throws against being charmed
     (when save-extras
       [:ul.extras
        (for [item save-extras]
          ^{:key (:id item)}
          [:li (:desc item)])])]))


; ======= Skills ===========================================

(def ^:private skills-table
  [[[:acrobatics "Acrobatics"]
    [:animal-handling "Animal Handling"]
    [:arcana "Arcana"]
    [:athletics "Athletics"]
    [:deception "Deception"]
    [:history "History"]
    [:insight "Insight"]
    [:intimidation "Intimidation"]
    [:investigation "Investigation"]]
   [[:medicine "Medicine"]
    [:nature "Nature"]
    [:perception "Perception"]
    [:performance "Performance"]
    [:persuasion "Persuasion"]
    [:religion "Religion"]
    [:sleight-of-hand "Sleight of Hand"]
    [:stealth "Stealth"]
    [:survival "Survival"]]])

(defn skill-box
  [label {:keys [ability modifier expert? half? proficient?]}]
  [:div.skill
   [:div.base-ability
    (str "(" (name ability) ")")]
   [:div.label label]
   [:div.score (mod->str modifier)]
   [:div.proficiency
    {:class [(when (and half?
                        (not (or expert? proficient?)))
               "half")
             (when (or expert? half? proficient?)
               "proficient")
             (when expert?
               "expert")]}]])

(defn skills-section []
  (let [skills (<sub [::subs/skill-info])]
    (->> skills-table
         (map
           (fn [col]
             [:div.skill-col
              (for [[skill-id label] col]
                ^{:key skill-id}
                [skill-box label (get skills skill-id)])]))
         (into [:div.sections]))))


; ======= Proficiencies ===================================

(defn proficiencies-section []
  [:div styles/proficiencies-section
   (when-let [proficiencies (seq (<sub [::subs/other-proficiencies]))]
     [:<>
      [:h3 "Proficiencies"]

      ; TODO organize by type
      [:div.section
       (for [f proficiencies]
         ^{:key (:id f)}
         [:div.item
          (:name f)])]])

   (when-let [languages (seq (<sub [::subs/languages]))]
     [:<>
      [:h3 "Languages"]

      [:div.section
       (for [f languages]
         ^{:key (:id f)}
         [:div.item
          (:name f)])]])])


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
    (:name s) ]

   (when (:consumes s)
     (when-let [{:keys [uses-left] :as info}
                (<sub [::subs/consumable s])]
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
    [:div.dmg
     (or (:base-dice s)
         (:dmg s))]
    (when-let [alt-dice (:alt-dice s)]
      [:div.dmg.alt
       "(" alt-dice ")"])]])

(defn- ammunition-block-for [w]
  (if-let [ammo (<sub [::subs/ammunition-for w])]
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

(defn- actions-combat []
  [:<>

   [:div.combat-info
    (for [info (<sub [::subs/combat-info])]
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
        [link>evt [:toggle-overlay [#'overlays/effect-info effect]]
         [:span.item
          (:name effect)]])])

   (when-let [s (<sub [::subs/unarmed-strike])]
     [:div.unarmed-strike
      [attack-block (assoc s :name "Unarmed Strike")]])

   (when-let [weapons (seq (<sub [::subs/equipped-weapons]))]
     [:div.weapons
      [:h4 "Weapons"]
      (for [w weapons]
        [:<> {:key (:id w)}
         [attack-block w]
         (when (:uses-ammunition? w)
           [ammunition-block-for w])])])

   (when-let [spell-attacks (seq (<sub [::subs/spell-attacks]))]
     [:div.spell-attacks
      [:h4 "Spell Attacks"]
      (for [s spell-attacks]
        ^{:key (:id s)}
        [attack-block s {:class :spell-attack}])])

   (when-let [attacks (seq (<sub [::subs/other-attacks]))]
     [:div.other
      [:h4 "Other Attacks"]
      (for [a attacks]
        ^{:key (:id a)}
        [attack-block a])])

   (when-let [actions (seq (<sub [::subs/special-combat-actions]))]
     [:div.special
      [:h4 "Special Attack Actions"]
      (for [a actions]
        ^{:key (:id a)}
        [:div.clickable
         {:on-click (click>evt [:toggle-overlay [#'overlays/info a]])}
         (:name a)])])
   ])

(defn- consume-use-block
  [consumable {:keys [omit-name]}]
  (when-let [use-id (:consumes consumable)]
    (when-let [{:keys [name uses-left] :as info}
               (<sub [::subs/consumable consumable])]
      (if (= :*spell-slot use-id)
        ; consuming spell slots is a special case
        [:div styles/consumable-use-block
         (if (<= uses-left 0)
           [:div.uses "0 spell slots left"]
           [:div.button
            {:on-click (click>evt [:toggle-overlay [#'overlays/info consumable]])}
            (str uses-left " spell slots left")])]

        ; normal case:
        [:div styles/consumable-use-block

         (when (not= name omit-name)
           [:div.name name])

         [:div.uses uses-left " left"]
         (when (> uses-left 0)
           [:div.button
            {:on-click (click>evt [::events/+use info 1])}
            "Use 1"])]))))

(defn- action-block [a]
  [:div.action
   [:div.name (:name a)]
   [consume-use-block a]
   [formatted-text :div.desc (:desc a)]])

(defn- actions-for-type [filter-type]
  (let [spells (seq (<sub [::subs/prepared-spells-filtered filter-type]))
        actions (seq (<sub [::subs/actions-for-type filter-type]))]
    (if (or spells actions)
      [:<>
       (when spells
         [:div.spells
          [:div.section-label "Spells"]

          (for [s spells]
            ^{:key (:id s)}
            [:div.spell-name.clickable
             {:on-click (click>evt [:toggle-overlay [#'overlays/spell-info s]])}
             (:name s)])])

       (when actions
         [:div.actions
          (for [a actions]
            ^{:key (:id a)}
            [action-block a])])]

      [:<> "Nothing available."])))

(defn- combat-page-link
  [page id label]
  (r/with-let [view-ref (atom nil)]
    (let [selected? (= id page)]
      (when selected?
        (when-let [r @view-ref]
          (.scrollIntoView r #js {:behavior "smooth"
                                  :block "nearest"
                                  :inline "center"})))

      [:div.filter {:class (when selected?
                             "selected")
                    :ref #(reset! view-ref %)}
       (if selected?
         [:span.unselectable label]

         [link>evt [::events/actions-page! id]
          label])])))

(defn- actions-page [id form]
  ^{:key id}
  [:div styles/swipeable-page
   form])

(declare limited-use-section)
(defn actions-section []
  (let [page (<sub [::subs/actions-page :combat])]
    [:<>
     [:div.filters
      [combat-page-link page :combat "Combat"]
      [combat-page-link page :actions "Actions"]
      [combat-page-link page :bonuses "Bonuses"]
      [combat-page-link page :reactions "Reactions"]
      [combat-page-link page :specials "Others"]
      [combat-page-link page :limited-use "Limited"]]

     [swipeable {:get-key #(<sub [::subs/actions-page :combat])
                 :set-key! #(>evt [::events/actions-page! %])}
      (actions-page :combat [actions-combat])
      (actions-page :actions [actions-for-type :action])
      (actions-page :bonuses [actions-for-type :bonus])
      (actions-page :reactions [actions-for-type :reaction])
      (actions-page :specials [actions-for-type :special-action])
      (actions-page :limited-use [limited-use-section])]]))


; ======= Features =========================================

(defn feature [f]
  (let [values (seq (:values f))]
    [:div.feature
     [:div.name (:name f)]

     [consume-use-block f {:omit-name (:name f)}]

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

    {:get #(:uses-left (<sub [::subs/limited-use (:id item)]))
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
  [uses trigger]
  (if (= 1 uses)
    (str "Once per " (trigger-labels trigger))
    (str uses " uses / " (trigger-labels trigger))))

(defn limited-use-section []
  (let [items (<sub [::subs/limited-use-configs])
        used (<sub [:limited-used])]
    [:div styles/limited-use-section
     (if-not (empty? items)
       (for [item items]
         (let [uses (:uses item)
               used-count (get used (:id item))]
           ^{:key (:id item)}
           [:div.limited-use
            [:div.info
             [:div.name (:name item)]
             [:div.recovery
              (describe-uses uses (:restore-trigger item))]]
            [usage-box item uses used-count]]))

       [:div.explanation
        [:p "Features, traits, and abilities that can only be used a limited number of times before requiring a rest will be listed here."]
        [:p "You will gain limited-use actions as you adventure and level up."]])
     ]))


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

(defn inventory-section []
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


; ======= Main interface ===================================

(defn- section
  ([title content]
   (section title nil content))
  ([title section-style content]
   (let [opts (or section-style
                  {})]
     [:div.section opts
      [:h1 title]
      [error-boundary
       content]])))

(defn- nav-link
  [page id label]
  ; NOTE: NOT a ratom, else we get an endless render loop
  (r/with-let [view-ref (atom nil)]
    (let [selected? (= id page)]
      (when selected?
        (when-let [r @view-ref]
          (.scrollIntoView r #js {:behavior "smooth"
                                  :block "nearest"
                                  :inline "center"})))
      [:h1.section
       {:class (when selected?
                 "selected")
        :on-click (click>evt [::events/page! id])
        :ref #(reset! view-ref %)}
       label])))

(defn- main-section
  "Call this as a function instead of a reagent form,
   since it adds the ^{:key}"
  [_page id opts content]
  ^{:key id}
  [:div.section opts
   content])

(defn- abilities-pane
  "This is the left side on desktop and tablets, or the
   first page/tab on mobile"
  []
  [:<>
   [abilities-section]

   [rest-buttons]

   [section "Skills"
    styles/skills-section
    [skills-section]]

   [proficiencies-section]])

(defn- sheet-right-page []
  (let [spellcasters (seq (<sub [::subs/spellcaster-blocks]))
        smartphone? (= :smartphone (<sub [:device-type]))
        page (<sub [::subs/page])]
    [:<>
     [:div.nav
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
         (main-section page :abilities
                       nil
                       [abilities-pane]))

       (main-section page :actions
                     styles/actions-section
                     [actions-section])

       (when spellcasters
         (main-section page :spells
                       styles/spells-section
                       [spells-section spellcasters]))

       (main-section page :inventory
                     styles/inventory-section
                     [inventory-section])

       (main-section page :features
                     styles/features-section
                     [features-section])

       ]] ]))

(defn sheet []
  [:div styles/container
   [error-boundary
    [header]]

   [:div styles/layout
    (when-not (= :smartphone (<sub [:device-type]))
      [error-boundary
       [:div.left.side
        [abilities-pane]]])

    [:div.right.side
     [sheet-right-page]]]])
