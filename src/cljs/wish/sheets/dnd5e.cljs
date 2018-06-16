(ns ^{:author "Daniel Leong"
      :doc "DND 5e sheet"}
  wish.sheets.dnd5e
  (:require [clojure.string :as str]
            [wish.util :refer [<sub click>evt]]
            [wish.sheets.dnd5e.subs :as dnd5e]
            [wish.sheets.dnd5e.events :as events]))

; ======= Utils ============================================

(defn hp
  []
  (let [sheet (<sub [:sheet])
        max-hp (<sub [::dnd5e/max-hp])]
    [:div.hp "HP"
     [:div.now (:hp sheet)]
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

(defn- ability->mod
  [score]
  (Math/floor (/ (- score 10) 2)))

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
  ; TODO skill proficiency/expertise
  (let [abilities (<sub [::dnd5e/abilities])
        proficiencies (<sub [::dnd5e/skill-proficiencies])
        prof-bonus (<sub [::dnd5e/proficiency-bonus])]
    (vec (cons
           :div.sections
           (map
             (fn [col]
               [:div.skill-col
                (for [[ability skill-id label] col]
                  (let [proficient? (contains? proficiencies skill-id)]
                    ^{:key skill-id}
                    [:div.skill
                     [:div.base-ability
                      (str "(" (name ability) ")")]
                     [:div.name label]
                     [:p.score
                      (+ (ability->mod (get abilities ability))
                         (when proficient?
                           ; TODO * expertise
                           prof-bonus))]
                     (when proficient?
                       [:p.proficient])]))])
             skills-table)))))


; ======= Combat ===========================================

(defn combat-section []
  [:div "TODO"])


; ======= Features =========================================

(defn- features-for
  [sub-vector]
  (->> (<sub sub-vector)
       (mapcat :features)
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
     [features-section]]]])
