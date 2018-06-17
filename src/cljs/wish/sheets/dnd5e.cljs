(ns ^{:author "Daniel Leong"
      :doc "DND 5e sheet"}
  wish.sheets.dnd5e
  (:require [clojure.string :as str]
            [wish.util :refer [<sub click>evt invoke-callable]]
            [wish.sheets.dnd5e.subs :as dnd5e]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.util :refer [ability->mod]]))

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

     [hp]]))

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
           [:td (ability->mod score)]
           ; TODO saving throws:
           [:td "save"]
           [:td (ability->mod score)]]))]]))


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
                        proficient? (contains? proficiencies skill-id)]
                    ^{:key skill-id}
                    [:div.skill
                     [:div.base-ability
                      (str "(" (name ability) ")")]
                     [:div.name label]
                     [:p.score
                      (+ (ability->mod (get abilities ability))
                         (cond
                           expert? (* 2 prof-bonus)
                           proficient?  prof-bonus))]
                     (when (or expert? proficient?)
                       [:p {:class (if expert?
                                     "expert"
                                     "proficient")}])]))])
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
          (if (>= to-hit 0)
            (str "+" to-hit)
            (str to-hit)))]
       [:div.dmg
        (:dmg s)]]])

   (when-let [spell-attacks (seq (<sub [::dnd5e/spell-attacks]))]
     (let [spell-attack-bonuses (<sub [::dnd5e/spell-attack-bonuses])]
       [:div.spell-attacks
        [:h4 "Spell Attacks"]
        (for [s spell-attacks]
          ^{:key (:id s)}
          [:div.attack.spell-attack
           [:div.name (:name s)]
           [:div.dice
            ; TODO
            ]
           [:div.to-hit
            (str "+" (get spell-attack-bonuses
                          (::dnd5e/source s)))]])]))])


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
    [:div
     [:div.rests
      [:div.short
       {:on-click (click>evt [:trigger-limited-use-restore :short-rest])}
       "Short Rest"]
      [:div.long
       {:on-click (click>evt [:trigger-limited-use-restore
                              [:short-rest :long-rest]])}
       "Long Rest"]]

     (for [item items]
       (let [uses (invoke-callable item :uses)]
         ^{:key (:id item)}
         [:div.limited-use
          [:div.name (:name item)]
          [:span.recovery
           (describe-uses uses (:restore-trigger item))]]))
     ]))


; ======= Spells ===========================================

(defn spell-block
  [s]
  (println "TODO more rendering for " s)
  [:div.spell
   (:name s)])

(defn spells-section []
  (let [spells (<sub [::dnd5e/class-spells])]
    [:div.spells
     ; TODO toggle only showing known/prepared
     (for [s spells]
       ^{:key (:id s)}
       [spell-block s])]))


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
