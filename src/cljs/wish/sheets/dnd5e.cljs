(ns ^{:author "Daniel Leong"
      :doc "DND 5e sheet"}
  wish.sheets.dnd5e
  (:require [clojure.string :as str]
            [wish.util :refer [<sub]]
            [wish.sheets.dnd5e.subs :as dnd5e]))

(defn hp
  []
  (let [sheet (<sub [:sheet])
        max-hp (<sub [::dnd5e/max-hp])]
    [:div.hp "HP"
     [:div.now (:hp sheet)]
     [:div.max  (str "/" max-hp)]]))

(defn header
  []
  (let [common (<sub [:sheet-meta])
        classes (<sub [:classes])]
    [:div.header "D&D"
     [:div.name (:name common)]
     [:div.classes (->> classes
                        (map (fn [c]
                               (str (-> c :data :name) " " (:level c))))
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

(defn skills-section
  []
  [:div ])


; ======= Public interface =================================

(defn sheet []
  [:div
   [header]
   [:div.sections
    [section "Abilities"
     [abilities-section]]
    [section "Skills"
     [skills-section]]]
   ])
