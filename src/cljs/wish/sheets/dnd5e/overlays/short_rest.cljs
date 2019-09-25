(ns wish.sheets.dnd5e.overlays.short-rest
  (:require-macros [wish.util :refer [fn-click]])
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.subs.abilities :as abilities]
            [wish.sheets.dnd5e.subs.hp :as hp]
            [wish.sheets.dnd5e.style :as styles]
            [wish.sheets.dnd5e.util :refer [->die-use-kw mod->str]]
            [wish.util :refer [<sub click>evts click>swap!]]
            [wish.views.widgets :refer-macros [icon]]
            [wish.views.widgets.fast-numeric]))

(defn- dice-pool [state]
  (let [dice-info (<sub [::hp/hit-dice])
        values (:values @state)]
    [:div.hit-dice-pool
     [:p "Your hit dice:"]
     (for [{:keys [die used total classes]} dice-info]
       (let [pending-uses (count (get values die))
             free-dice (- total used pending-uses)]
         ^{:key die}
         [:div.hit-die
          {:on-click (fn-click
                       (when (> free-dice 0)
                         (let [next-id (:next-id
                                         (swap! state update :next-id inc))]
                           (swap! state assoc-in [:values die next-id] nil))))}

          (str "D" die " (" (str/join ", " classes) ") ")
          [:div.desc
           free-dice " / " total " left"]
          ]))]))

(defn- die-usage
  [state & {:keys [id max-val con-mod placeholder removable?
                   ; only if removable:
                   die i]}]
  [bind-fields
   [:div.hit-die-use
    [:input.hit-die-value {:field :fast-numeric
                           :id id
                           :placeholder placeholder
                           :min 1
                           :max max-val}]
    (when con-mod
      [:span.mod con-mod])

    (when removable?
      [:a {:href "#"
           :tabIndex -1
           :on-click (click>swap! state #(update-in % [:values die] dissoc i))}
       (icon :remove-circle)])]

   state])

(defn- dice-usage [state]
  (let [con-mod (-> (<sub [::abilities/modifiers])
                    :con
                    mod->str)]
    (when-let [values (seq (:values @state))]
      (when (some #(seq (second %)) values)
        [:div.dice-usage
         [:p "Hit dice usage"]
         (for [[die id->roll] values
               i (keys id->roll)]
           ^{:key (str die "/" i)}
           [die-usage state
            :id [:values die i]
            :max-val die
            :con-mod con-mod
            :placeholder (str "D" die)
            :removable? true
            :die die
            :i i])

         ; field for extra HP (eg: from Song of Rest)
         [die-usage state
          :id :extra
          :max-val 50
          :placeholder "Extra?"]
         ]))))


; ======= public interface ================================

(defn overlay []
  (r/with-let [state (r/atom {:next-id 0})]
    (let [current-state @state
          amount-to-heal (when-let [dice-totals (->> current-state
                                                     :values
                                                     vals
                                                     (mapcat vals)
                                                     seq)]
                           (let [dice-sum (apply + dice-totals)]
                             (when (> dice-sum 0)
                               (let [con-mod (:con (<sub [::abilities/modifiers]))
                                     dice-used (->> dice-totals
                                                    (keep identity)
                                                    count)]
                                 (+ (* dice-used con-mod)
                                    (:extra current-state)
                                    dice-sum)))))]
      [:div (styles/short-rest-overlay)
       [:h5 "Short Rest"]

       ; SRD description:
       [:p.desc "A short rest is a period of downtime, at least 1 hour long, during which a character does nothing more strenuous than eating, drinking, reading, and tending to wounds."]
       [:p.desc "Tap on hit dice below to use them as part of your short rest."]

       ; TODO support auto-rolling hit dice
       [:div.sections
        [dice-pool state]
        [dice-usage state]]

       ; TODO support (or at least surface) things like arcane recovery?

       [:div.button
        {:on-click (click>evts [:trigger-limited-use-restore :short-rest]
                               [:+uses (reduce-kv
                                         (fn [m die-size rolls]
                                           (assoc m
                                                  (->die-use-kw
                                                    die-size)
                                                  (->> rolls
                                                       (keep second)
                                                       count)))
                                         {}
                                         (:values @state))]
                               [::events/update-hp
                                amount-to-heal
                                (<sub [::hp/max])]
                               [:toggle-overlay nil])}
        "Take a short rest"
        (when (> amount-to-heal 0)
          (str "; heal +" amount-to-heal))]])))

