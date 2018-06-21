(ns ^{:author "Daniel Leong"
      :doc "DND 5e sheet"}
  wish.sheets.dnd5e
  (:require [clojure.string :as str]
            [cljs-css-modules.macro :refer-macros [defstyle]]
            [wish.util :refer [<sub click>evt invoke-callable]]
            [wish.sheets.dnd5e.subs :as dnd5e]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.util :refer [ability->mod mod->str]]
            [wish.views.widgets :as widgets :refer-macros [icon] :refer [link]]))


; ======= CSS ==============================================

(def color-proficient "#77E731")
(def color-expert "#E8E154")

; TODO we should maybe just provide global styles with the
; right fallbacks
(def flex {:display 'flex})
(def flex-vertical (merge
                     flex
                     {:flex-direction 'column}))
(def flex-center (merge
                   flex
                   {:align-items 'center}))
(def flex-grow {:flex-grow 1})

(def button {:cursor 'pointer})

(def metadata {:font-size "10pt"})

(defstyle styles
  [:.limited-use-section
   [:.rests flex-center
    [:.button (merge
                flex-grow
                button
                {:text-align 'center})]]
   [:.limited-use (merge
                    flex-center
                    {:padding "4px"})
    [:.info flex-grow
     [:.recovery metadata]]
    [:.usage
     [:.button (merge
                 button
                 {:padding "4px"})]]]]

  [:.spells-section
   [:.spell-slots-use flex
    [:.slot {:width "24px"
             :height "24px"
             :border "1px solid #333"
             :margin "4px"}
     [:&.used {:cursor 'pointer}]]]

   [:.spell-slot-level flex-center
    [:.label flex-grow]]

   [:.spell
    [:.name {:font-weight "bold"}]
    [:.meta metadata]]]

  [:.skill-col (merge
                 flex-vertical
                 flex-grow)
   [:.skill flex
    [:.label flex-grow]
    [:.score {:padding "0 4px"}]
    [:.proficiency
     {:color color-proficient
      :padding-right "12px"}
     [:&::before
      {:content "' '"}]  ; en-space unicode
     [:&.proficient::before
      {:content "'●'"}]
     [:&.expert::before
      {:color color-expert}]]]])

; ======= Utils ============================================

(defn hp
  []
  (let [[hp max-hp] (<sub [::dnd5e/hp]) ]
    [:div.hp "HP"
     [:div.now hp]
     [:div.max  (str "/" max-hp)]
     [:a
      {:href "#"
       :on-click (click>evt [::events/update-hp])}
      "Test"]]))

(defn header
  []
  (let [common (<sub [:sheet-meta])
        classes (<sub [:classes])]
    [:div.header "D&D"
     [:div.name (:name common)]
     [:div.classes (->> classes
                        (map (fn [c]
                               (str (-> c :name) " " (:level c))))
                        (str/join " / "))]
     [:div.race (:name (<sub [:race]))]

     [hp]

     [widgets/save-state]

     (let [sheet-id (<sub [:active-sheet-id])]
       [link {:href (str "/sheets/" (namespace sheet-id)
                         "/" (name sheet-id) "/builder")}
        (icon :settings)])]))

(defn section
  [title & content]
  (apply vector
         :div.section
         [:h1 title]
         content))


; ======= sections =========================================

(def labeled-abilities
  [[:str "Strength"]
   [:dex "Dexterity"]
   [:con "Constitution"]
   [:int "Intelligence"]
   [:wis "Wisdom"]
   [:cha "Charisma"]])

(defn abilities-section
  []
  (let [abilities (<sub [::dnd5e/abilities])]
    [:table.abilities
     [:tbody
      (for [[id label] labeled-abilities]
        (let [score (get abilities id)]
          ^{:key id}
          [:tr
           [:td score]
           [:td label]
           [:td "mod"]
           [:td (mod->str
                  (ability->mod score))]
           ; TODO saving throws:
           [:td "save"]
           [:td (mod->str
                  (ability->mod score))]]))]]))


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
                   "expert"))}]] )

(defn skills-section []
  (let [abilities (<sub [::dnd5e/abilities])
        expertise (<sub [::dnd5e/skill-expertise])
        proficiencies (<sub [::dnd5e/skill-proficiencies])
        prof-bonus (<sub [::dnd5e/proficiency-bonus])]
    (vec (cons
           :div.sections
           (map
             (fn [col]
               [:div {:class (:skill-col styles)}
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

(defn combat-section []
  [:div

   (when-let [s (<sub [::dnd5e/unarmed-strike])]
     [:div.unarmed-strike
      [:div.attack
       [:div.name "Unarmed Strike"]
       [:div.to-hit
        (let [{:keys [to-hit]} s]
          (mod->str to-hit))]
       [:div.dmg
        (:dmg s)]]])

   (when-let [spell-attacks (seq (<sub [::dnd5e/spell-attacks]))]
     [:div.spell-attacks
      [:h4 "Spell Attacks"]
      (for [s spell-attacks]
        ^{:key (:id s)}
        [:div.attack.spell-attack
         [:div.name (:name s)]
         [:div.dice (:base-dice s) ]
         [:div.to-hit
          (mod->str (:to-hit s))]])])])


; ======= Features =========================================

; TODO these should probably be subscriptions
(defn- features-for
  [sub-vector]
  (->> (<sub sub-vector)
       (mapcat (comp vals :features))
       (remove :implicit?)
       seq))

(defn feature
  [f]
  [:div.feature
   [:div.name (:name f)]
   [:div.desc (:desc f)]])

(defn features-section []
  (vec
    (cons
      :div.features
      [(when-let [fs (features-for [:classes])]
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
       ])))


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
    [:div {:class (:limited-use-section styles)}
     [:div.rests
      [:div.button.short
       {:on-click (click>evt [:trigger-limited-use-restore :short-rest])}
       "Short Rest"]
      [:div.button.long
       {:on-click (click>evt [:trigger-limited-use-restore
                              [:short-rest :long-rest]])}
       "Long Rest"]]

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
     ]))


; ======= Spells ===========================================

(defn spell-block
  [s]
  (let [level (:spell-level s)]
    [:div.spell
     [:div.name (:name s)]

     ; TODO concentration? ritual?
     [:div.meta (if (= 0 level)
                  "Cantrip"
                  (str "Level " level))]

     [:div.detail
      (when (:dice s)
        [:div.dice
         (invoke-callable s :dice)])

      ; TODO format the desc text w/line breaks, bold, etc.
      [:div.desc (:desc s)]]]))

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

(defn spells-section []
  (let [spells (<sub [::dnd5e/class-spells])
        slots-sets (<sub [::dnd5e/spell-slots])
        slots-used (<sub [::dnd5e/spell-slots-used])]
    [:div {:class (:spells-section styles)}
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


; ======= Public interface =================================

(defn sheet []
  [:div
   [header]
   [:div.sections
    [section "Abilities"
     [abilities-section]]
    [section "Skills"
     [skills-section]]
    [section "Combat"
     [combat-section]]

    [section "Features"
     [features-section]]

    [section "Limited-use"
     [limited-use-section]]
    [section "Spells"
     [spells-section]]]])
