(ns ^{:author "Daniel Leong"
      :doc "Shared widgets for dnd5e sheet "}
  wish.sheets.dnd5e.widgets
  (:require [clojure.string :as str]
            [wish.util :refer [<sub click>evt invoke-callable]]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.style :refer [styles]]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
             :refer [formatted-text]]))

(defn stringify-components
  [{components :comp}]
  (letfn [(vs-parts [k]
            (case k
              :v "V"
              :s "S"
              :vs "V, S"))
          (m-part [p]
            (str "M (" p ")"))]
    (cond
      (keyword? components)
      (vs-parts components)

      ; are any material only?
      (string? components)
      (m-part components)

      (= 1 (count components))
      (m-part (first components))

      :else
      (str (vs-parts (first components))
           ", "
           (m-part (second components))))))

(defn- stringify-dam-type
  [t]
  ; TODO table?
  (when t
    (-> t
        name
        str/capitalize
        (str/replace "-" " "))))

(defn currency-preview []
  (let [{:keys [platinum gold silver electrum copper]} (<sub [::subs/currency])
        any? (> (+ platinum gold silver electrum copper)
                0)]
    [:span {:class (:currency-preview styles)}
     "Currency"

     (when-not any?
       " (just some lint)")

     (when (> platinum 0)
       [:div.pair
        platinum
        [:span.currency.p "P"]])
     (when (> gold 0)
       [:div.pair
        gold
        [:span.currency.g "G"]])
     (when (> electrum 0)
       [:div.pair
        electrum
        [:span.currency.e "E"]])
     (when (> silver 0)
       [:div.pair
        silver
        [:span.currency.s "S"]])
     (when (> copper 0)
       [:div.pair
        copper
        [:span.currency.c "C"]])]))

(defn spell-aoe
  "Renders the AOE of a spell"
  [[kind l w]]
  [:span
   (case kind
     :cylinder (str l "-foot-radius, " w " ft. tall cylinder")

     :circle (str l "-foot-radius circle")
     :cone (str l " ft. cone")
     :sphere (str l "-foot-radius sphere")

     :cube (str l " ft. cube")
     :line (str l " ft. long, " w " ft. wide line")
     :square (str l " ft. square"))
   ])

(defn spell-tags
  "Render a series of 'tags' indicating things like whether
   the spell is a Ritual, or requires Concentration."
  [{:keys [con? rit?]}]
  (when (or con? rit?)
    [:span {:class (:spell-tags styles)}
     (when con?
       [:span.tag "C"])
     (when rit?
       [:span.tag "R"])
     ]))

(defn spell-card
  "Spell info card widget"
  [s]
  [:div {:class (:spell-card styles)}
   [:table.info
    [:tbody
     [:tr
      [:th.header "Casting Time"]
      [:td (:time s)]]
     [:tr
      [:th.header "Range"]
      [:td (:range s)]]

     (when-let [aoe (:aoe s)]
       [:tr
        [:th.header "Area of Effect"]
        [:td [spell-aoe aoe]]])

     [:tr
      [:th.header "Components"]
      [:td (stringify-components s)]]
     [:tr
      [:th.header "Duration"]
      [:td (:duration s)]]

     (when-let [tags (spell-tags s)]
       [:tr
        [:th.header "Properties"]
        [:td tags]])

     (when-let [dice (:dice s)]
       (let [{:keys [damage]} s]
         [:tr
          [:th.header
           (if damage
             "Damage"
             "Healing")]

          [:td
           [:u
            (invoke-callable
              (update s :spell-mod #(or % "(spell mod)"))
              :dice)]

           (if-let [dam-type (stringify-dam-type damage)]
             (str " " dam-type)
             " HP")]]))

     (when-let [save (:save s)]
       [:tr
        [:th.header
         "Saving Throw"]
        [:td
         (case save
           :str "Strength"
           :dex "Dexterity"
           :con "Constitution"
           :int "Intelligence"
           :wis "Wisdom"
           :cha "Charisma")
         (when-let [dc (:save-dc s)]
           (str " (" dc ")"))]])
     ]]

   [formatted-text :div.desc (:desc s)]])
