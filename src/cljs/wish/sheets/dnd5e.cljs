(ns ^{:author "Daniel Leong"
      :doc "DND 5e sheet"}
  wish.sheets.dnd5e
  (:require [clojure.string :as str]
            [wish.util :refer [<sub click>evt invoke-callable]]
            [wish.util.nav :refer [sheet-url]]
            [wish.sheets.dnd5e.overlays :as overlays]
            [wish.sheets.dnd5e.style :refer [styles]]
            [wish.sheets.dnd5e.subs :as dnd5e]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.util :refer [ability->mod mod->str]]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
             :refer [expandable formatted-text link]]))

; ======= Top bar ==========================================

(defn hp []
  (let [[hp max-hp] (<sub [::dnd5e/hp]) ]
    [:div.clickable.hp.col

     {:on-click (click>evt [:toggle-overlay [#'overlays/hp-overlay]])}

     [:div.label "Hit Points"]
     [:div.now (str hp " / " max-hp)]]))

(defn header []
  (let [common (<sub [:sheet-meta])
        classes (<sub [:classes])]
    [:div {:class (:header styles)}
     [:div.left.side
      [:div.col
       [widgets/save-state]]

      [:div.col.left
       [:div.name [link {:href "/sheets"}
                   (:name common)]]
       [:div.meta
        [:div.race (:name (<sub [:race]))]
        [:div.classes (->> classes
                           (map (fn [c]
                                  (str (-> c :name) " " (:level c))))
                           (str/join " / "))]]]]

     [:div.space]

     [:div.right.side
      [:div.col
       [:div.stat (mod->str
                    (<sub [::dnd5e/proficiency-bonus]))]
       [:div.label "Proficiency"]]

      [:div.col
       [:div.stat (<sub [::dnd5e/ac])]
       [:div.label "AC"]]

      [:div.col
       [:div.stat (<sub [::dnd5e/speed]) [:span.unit " ft"]]
       [:div.label "Base Speed"]]

      [:div.col
       [:div.stat (<sub [::dnd5e/passive-perception])]
       [:div.label "Pass. Perc."]]

      [:div.col
       [:div.stat (mod->str
                    (<sub [::dnd5e/initiative]))]
       [:div.label "Initiative"]]

      [:div.col
       [hp]]

      [:div.col
       (let [sheet-id (<sub [:active-sheet-id])]
         [link {:href (sheet-url sheet-id :builder :class)}
          (icon :settings)])]]]))


; ======= abilities ========================================

(def labeled-abilities
  [[:str "Strength"]
   [:dex "Dexterity"]
   [:con "Constitution"]
   [:int "Intelligence"]
   [:wis "Wisdom"]
   [:cha "Charisma"]])

(defn abilities-section []
  (let [abilities (<sub [::dnd5e/abilities])
        prof-bonus (<sub [::dnd5e/proficiency-bonus])
        save-proficiencies (<sub [::dnd5e/save-proficiencies])]
    [:<>
     (for [[id label] labeled-abilities]
       (let [score (get abilities id)
             modifier (ability->mod score)
             modifier-str (mod->str modifier)
             proficient? (get save-proficiencies id)
             save-str (if proficient?
                        (mod->str
                          (+ modifier prof-bonus))

                        modifier-str)]
         ^{:key id}
         [:div.ability
          [:div.score score]
          [:div.label label]
          [:div.info "mod"]
          [:div.mod modifier-str]
          [:div.info "save"]
          [:div.mod save-str]
          [:div.proficiency
           {:class (when proficient?
                     "proficient")}]]))

     ; TODO This is a good place for things like Elven advantage
     ; on saving throws against being charmed
     ]))


; ======= Skills ===========================================

(def ^:private skills-table
  [[[:dex :acrobatics "Acrobatics"]
    [:wis :animal-handling "Animal Handling"]
    [:int :arcana "Arcana"]
    [:str :athletics "Athletics"]
    [:cha :deception "Deception"]
    [:int :history "History"]
    [:wis :insight "Insight"]
    [:cha :intimidation "Intimidation"]
    [:int :investigation "Investigation"]]
   [[:wis :medicine "Medicine"]
    [:int :nature "Nature"]
    [:wis :perception "Perception"]
    [:cha :performance "Performance"]
    [:cha :persuasion "Persuasion"]
    [:int :religion "Religion"]
    [:dex :sleight-of-hand "Sleight of Hand"]
    [:dex :stealth "Stealth"]
    [:wis :survival "Survival"]]])

(defn skill-box
  [ability label total-modifier expert? proficient?]
  [:div.skill
   [:div.base-ability
    (str "(" (name ability) ")")]
   [:div.label label]
   [:div.score
    (mod->str
      total-modifier)]
   [:div.proficiency
    {:class (str (when (or expert? proficient?)
                   "proficient ")
                 (when expert?
                   "expert"))}]])

(defn skills-section []
  (let [abilities (<sub [::dnd5e/abilities])
        expertise (<sub [::dnd5e/skill-expertise])
        proficiencies (<sub [::dnd5e/skill-proficiencies])
        prof-bonus (<sub [::dnd5e/proficiency-bonus])]
    (vec (cons
           :div.sections
           (map
             (fn [col]
               [:div.skill-col
                (for [[ability skill-id label] col]
                  (let [expert? (contains? expertise skill-id)
                        proficient? (contains? proficiencies skill-id)
                        total-modifier (+ (ability->mod (get abilities ability))
                                          (cond
                                            expert? (* 2 prof-bonus)
                                            proficient?  prof-bonus))]
                    ^{:key skill-id}
                    [skill-box ability label total-modifier expert? proficient?]))])
             skills-table)))))


; ======= Combat ===========================================

(defn- attack-block [s & [extra-info]]
  [:div.attack (or extra-info {})
   [:div.name (:name s)]

   [:div.info-group
    [:div.label
     "TO HIT"]
    [:div.to-hit
     (mod->str (:to-hit s))]]

   [:div.info-group
    [:div.label
     "DMG"]
    [:div.dmg
     (or (:base-dice s)
         (:dmg s))]]] )

(defn combat-section []
  [:<>

   (when-let [s (<sub [::dnd5e/unarmed-strike])]
     [:div.unarmed-strike
      [attack-block (assoc s :name "Unarmed Strike")]])

   (when-let [spell-attacks (seq (<sub [::dnd5e/spell-attacks]))]
     [:div.spell-attacks
      [:h4 "Spell Attacks"]
      (for [s spell-attacks]
        ^{:key (:id s)}
        [attack-block s {:class :spell-attack}])])])


; ======= Features =========================================

; TODO these should definitely be subscriptions
(defn- features-for
  [sub-vector]
  (->> (<sub sub-vector)
       (mapcat (comp vals :features))
       (filter :name)
       (remove :implicit?)
       seq))

(defn feature
  [f]
  [expandable
   [:div.feature
    [:div.name (:name f)]]
   [formatted-text :div.desc (:desc f)]])

(defn features-section []
  [:<>
   (when-let [fs (features-for [:classes])]
      [:div.features-category
       [:h3 "Class features"]
       (for [f fs]
         ^{:key (:id f)}
         [feature f])])

    (when-let [fs (features-for [:races])]
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
    :on-click (click>evt [:toggle-used (:id item)])}
   (if used?
     "Used"
     "Use")])

(defn usage-box
  "Render some sort of box for 'using' a limited-use item,
   appropriate to the number of total possible `uses` and
   indicating the current `used-count`."
  [item uses used-count]
  [:div.usage
   (cond
     (= 1 uses) [usage-box-single item (> used-count 0)]
     :else (do
             (println "Handle " uses " uses")
             [:div (str (- uses used-count)
                        " uses remaining")]))])


(def trigger-labels
  {:short-rest "Short Rest"
   :long-rest "Long Rest"})

(defn describe-uses
  [uses trigger]
  (if (= 1 uses)
    (str "Once per " (trigger-labels trigger))
    (str uses " uses / " (trigger-labels trigger))))

(defn limited-use-section [items]
  (let [items (<sub [::dnd5e/limited-uses])
        used (<sub [:limited-used])]
    [:<>
     [:div.rests
      [:div.button.short
       {:on-click (click>evt [:toggle-overlay [#'overlays/short-rest-overlay]])}
       "Short Rest"]
      [:div.button.long
       {:on-click (click>evt [:trigger-limited-use-restore
                              [:short-rest :long-rest]])}
       "Long Rest"]]

     (if-not (empty? items)
       (for [item items]
         (let [uses (invoke-callable item :uses)
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
  (let [level (:spell-level s)]
    [expandable
     [:div.spell
      [:div.spell-info
       [:div.name (:name s)]

       ; TODO concentration? ritual?
       [:div.meta (if (= 0 level)
                    "Cantrip"
                    (str "Level " level))]]

      (when (:dice s)
        [:div.dice
         (invoke-callable s :dice)])]

     ; collapsed:
     [:div.detail
      [formatted-text :div.desc (:desc s)]]]))

(defn spell-slot-use-block
  [kind level total used]
  [:div.spell-slots-use
   {:on-click (click>evt [::events/use-spell-slot kind level total])}
   (for [i (range total)]
     (let [used? (< i used)]
       ^{:key (str level "/" kind i)}
       [:div.slot
        {:class (when used?
                  "used")
         :on-click (when used?
                     (click>evt [::events/restore-spell-slot kind level total]
                                :propagate? false))}
        (when used?
          (icon :close))]))])

(defn spells-section [spells]
  (let [slots-sets (<sub [::dnd5e/spell-slots])
        slots-used (<sub [::dnd5e/spell-slots-used])]
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

     [:div.spells
      [:h4 "Available spells"]
      ; TODO toggle only showing known/prepared
      (for [s spells]
        ^{:key (:id s)}
        [spell-block s])]]))


; ======= Main interface ===================================

(defn- section
  ([title content]
   (section title nil content))
  ([title section-style content]
   (let [opts (if section-style
                {:class (section-style styles)}
                {})]
     [:div.section opts
      [:h1 title]
      content])))

(defn sheet []
  [:<>
   [header]
   [:div.sections
    [section "Abilities"
     :abilities
     [abilities-section]]
    [section "Skills"
     :skills
     [skills-section]]
    [section "Combat"
     :combat
     [combat-section]]

    [section "Features"
     [features-section]]

    [section "Limited-use"
     :limited-use-section
     [limited-use-section]]

    (when-let [spells (seq (<sub [::dnd5e/class-spells]))]
      [section "Spells"
       :spells-section
       [spells-section spells]])]])
