(ns ^{:author "Daniel Leong"
      :doc "Overlays"}
  wish.sheets.dnd5e.overlays
  (:require [clojure.string :as str]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.subs :as dnd5e]
            [wish.sheets.dnd5e.style :refer [styles]]
            [wish.util :refer [<sub click>evt click>evts]]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
             :refer [formatted-text link]]))

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

(defn short-rest-overlay []
  [:div {:class (:short-rest-overlay styles)}
   [:h5 "Short Rest"]

   ; SRD description:
   [:p.desc "A short rest is a period of downtime, at least 1 hour long, during which a character does nothing more strenuous than eating, drinking, reading, and tending to wounds."]

   ; TODO support consuming hit dice, with input fields to adjust HP
   ; accordingly.
   (let [dice-info (<sub [::dnd5e/hit-dice])]
     [:div.hit-dice
      [:p "Your hit dice:"]
      (for [{:keys [die uses total classes]} dice-info]
        ^{:key die}
        [:div.hit-die
         (str "D" die " (" (str/join ", " classes) ")")
         [:p (str (- total uses) " left")]
         ])])

   ; TODO support (or at least surface) things like arcane recovery?

   [:div.button
    {:on-click (click>evts [:trigger-limited-use-restore :short-rest]
                           [:toggle-overlay nil])}
    "Take a short rest"]])
