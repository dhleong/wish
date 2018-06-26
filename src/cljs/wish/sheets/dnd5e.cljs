(ns ^{:author "Daniel Leong"
      :doc "DND 5e sheet"}
  wish.sheets.dnd5e
  (:require [clojure.string :as str]
            [cljs-css-modules.macro :refer-macros [defstyle]]
            [wish.util :refer [<sub click>evt invoke-callable]]
            [wish.util.nav :refer [sheet-url]]
            [wish.sheets.dnd5e.subs :as dnd5e]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.util :refer [ability->mod mod->str]]
            [wish.style.flex :as flex :refer [flex]]
            [wish.style.shared :refer [metadata]]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
             :refer [expandable formatted-text link]]))


; ======= CSS ==============================================

(def color-proficient "#77E731")
(def color-expert "#E8E154")

(def button {:cursor 'pointer})

(def proficiency-style
  [:.proficiency
   {:color color-proficient
    :padding-right "12px"}
   [:&::before
    {:content "'●'"
     :visibility 'hidden}]
   [:&.proficient::before
    {:visibility 'visible}]
   [:&.expert::before
    {:color color-expert}]])

(defstyle styles
  [:.header (merge flex
                   {:background "#666666"
                    :color "#f0f0f0"
                    :padding "4px 12px"})
   [:.side flex
    [:.col (merge flex/vertical-center
                  {:padding "4px 8px"
                   :text-align 'center})
     [:&.left {:text-align 'left}]

     [:.meta (merge flex
                    metadata)
      [:.race {:margin-right "0.5em"}]]

     [:.save-state {:margin-right "12px"}]

     [:.stat {:font-size "140%"}
      [:.unit {:font-size "60%"}]]]]

   [:.label {:font-size "80%"}]

   [:.hp {:align-items 'center}
    [:.now {:padding "4px"
            :font-size "120%"
            :text-align 'center}]]

   [:.space flex/grow]]

  [:.hp-overlay {:width "300px"}
   [:.current-hp {:width "5em"
                  :font-size "1.2em"
                  :text-align 'center}]]

  [:.abilities
   [:.ability (merge flex
                     flex/align-center
                     {:height "1.7em"})
    [:.score {:font-size "1.1em"
              :width "1.9em"}]
    [:.label flex/grow]
    [:.info (merge metadata
                   {:padding "0 4px"})]
    [:.mod {:font-size "1.1em"
            :padding-right "12px"}]
    proficiency-style]]

  [:.skills
   [:.skill-col (merge
                  flex/vertical
                  flex/grow)
    [:.skill (merge flex
                    flex/wrap
                    {:padding "2px 0"})
     [:.base-ability (merge metadata
                            {:width "100%"})]
     [:.label flex/grow]
     [:.score {:padding "0 4px"}]

     proficiency-style]]]

  [:.combat
   [:.attack flex/center
    [:.name flex/grow]
    [:.info-group (merge flex/center
                         flex/vertical-center
                         {:padding "4px"})
     [:.label {:font-size "60%"}]]]]

  [:.limited-use-section
   [:.rests flex/center
    [:.button (merge
                flex/grow
                button
                {:text-align 'center})]]
   [:.limited-use (merge
                    flex/center
                    {:padding "4px"})
    [:.info flex/grow
     [:.recovery metadata]]
    [:.usage
     [:.button (merge button)
      [:&.selected {:background-color "#ddd"}
       [:&:hover {:background-color "#eee"}]]]]]]

  [:.spells-section
   [:.spell-slots-use flex
    [:.slot {:width "24px"
             :height "24px"
             :border "1px solid #333"
             :margin "4px"}
     [:&.used {:cursor 'pointer}]]]

   [:.spell-slot-level flex/center
    [:.label flex/grow]]

   [:.spell flex/center
    [:.spell-info flex/grow
     [:.name {:font-weight "bold"}]
     [:.meta metadata]]
    [:.dice {:align-self 'center}]]])


; ======= Top bar ==========================================

(defn hp-overlay []
  (let [[hp max-hp] (<sub [::dnd5e/hp])]
    [:div {:class (:hp-overlay styles)}
     [:h5 "Hit Points"]
     [:div.sections
      [:a {:href "#"
           :on-click (click>evt [::events/update-hp -1 max-hp])}
       (icon :remove-circle)]

      ; TODO support for tmp hp, temporarily modified max hp,
      ; boxes to input damage/healing, and possibly a "deferred update"
      ; mechanism somehow.
      ; TODO track death saving throw successes/failures
      [:div.current-hp hp]

      [:a {:href "#"
           :on-click (click>evt [::events/update-hp 1 max-hp])}
       (icon :add-circle)]]]))

(defn hp []
  (let [[hp max-hp] (<sub [::dnd5e/hp]) ]
    [:div.clickable.hp.col

     {:on-click (click>evt [:toggle-overlay [#'hp-overlay]])}

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
       {:on-click (click>evt [:trigger-limited-use-restore :short-rest])}
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
