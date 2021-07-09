(ns wish.sheets.dnd5e.overlays.generic-info
  (:require [clojure.string :as str]
            [wish.sheets.dnd5e.subs.proficiency :as proficiency]
            [wish.sheets.dnd5e.util :refer [ability->mod]]
            [wish.sheets.dnd5e.widgets :refer [spell-aoe]]
            [wish.util :refer [<sub]]
            [wish.views.widgets :refer [formatted-text-fragment]]))


; ======= Item/Spell generic info =========================

(def ^:private properties
  {:finesse? "Finesse"
   :heavy? "Heavy"
   :light? "Light"
   :reach? "Reach"
   :special? "Special"
   :two-handed? "Two-handed"
   :uses-ammunition? "Uses Ammunition"
   :versatile "Versatile"})

(defn generic-info [entity]
  (let [{:keys [aoe damage dice range]} entity
        proficiency-bonus (<sub [::proficiency/bonus])]
    (when (or aoe damage dice range)
      [:table.info
       [:tbody
        (when-let [cast-time (:time entity)]
          [:tr
           [:th.header "Cast Time"]
           [:td cast-time]])

        (when range
          [:tr
           [:th.header "Range"]
           (if (string? range)
             [:td range]
             (let [[near far] range]
               [:td near " / " far " ft."]))])

        (when aoe
          [:tr
           [:th.header "Area of Effect"]
           [:td [spell-aoe aoe]]])

        (when-let [flags (->> properties
                              keys
                              (filter entity)
                              (map properties)
                              seq)]
          [:tr
           [:th.header "Properties"]
           [:td (str/join "; " flags)]])

        (when damage
          [:tr
           [:th.header "Damage Type"]
           [:td (str/capitalize
                  (name damage))]])

        (when dice
          [:tr
           [:th.header (if damage
                         "Damage"
                         "Healing")]
           [:td (if (fn? dice)
                  (dice (assoc (:wish/container entity)
                               :proficiency-bonus proficiency-bonus))
                  dice)]])
        ]]
      )))


; ======= Ally info =======================================

(def ^:private abilities-block
  (delay
    (resolve
     'wish.sheets.dnd5e.views.abilities/abilities-block)))

(defn- ->abilities-info [raw-abilities]
  (reduce-kv
    (fn [m id score]
      (let [modifier (ability->mod score)]
        (assoc m id {:score score
                     :modifier modifier
                     :save modifier})))
    {}
    raw-abilities))

(defn- prefixed-formatted-text [label text]
  [:div.desc
   [formatted-text-fragment {:first-container [:div.p [:b label]]}
    text]])

(defn ally [entity]
  [:<>
   (when (:size entity)
     [:div.desc
      (str/capitalize (name (:size entity)))
      " "
      (str/capitalize (name (:type entity)))])

   (when-some [abilities (:abilities entity)]
     [@abilities-block
      :abilities (->abilities-info abilities)])

   (when-some [senses (:senses entity)]
     [prefixed-formatted-text "Senses: " senses])
   (when-some [speed (:speed entity)]
     [prefixed-formatted-text "Speed: " speed])])
