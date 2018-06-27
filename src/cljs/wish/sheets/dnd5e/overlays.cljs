(ns ^{:author "Daniel Leong"
      :doc "Overlays"}
  wish.sheets.dnd5e.overlays
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.subs :as dnd5e]
            [wish.sheets.dnd5e.style :refer [styles]]
            [wish.sheets.dnd5e.util :refer [->die-use-kw mod->str]]
            [wish.util :refer [<sub click>evt click>evts]]
            [wish.views.util :refer [dispatch-change-from-keyup]]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
             :refer [formatted-text link]]
            [wish.views.widgets.fast-numeric]))

; ======= hit points =======================================

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


; ======= short rest =======================================

(defn dice-pool [state]
  (let [dice-info (<sub [::dnd5e/hit-dice])
        values (:values @state)]
    [:div.hit-dice-pool
     [:p "Your hit dice:"]
     (for [{:keys [die used total classes]} dice-info]
       (let [pending-uses (count (get values die))
             free-dice (- total used pending-uses)]
         ^{:key die}
         [:div.hit-die
          {:on-click (fn [e]
                       (.preventDefault e)
                       (when (> free-dice 0)
                         (let [next-id (:next-id
                                         (swap! state update :next-id inc))]
                           (swap! state assoc-in [:values die next-id] nil))))}

          (str "D" die " (" (str/join ", " classes) ") ")
          [:div.desc
           free-dice " / " total " left"]
          ]))]))

(defn die-usage
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
           :on-click (fn [e]
                       (.preventDefault e)

                       (swap! state #(update-in % [:values die] dissoc i)))}
       (icon :remove-circle)])]

   state])

(defn dice-usage [state]
  (let [con-mod (-> (<sub [::dnd5e/ability-modifiers])
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

(defn short-rest-overlay []
  (let [state (r/atom {:next-id 0})]
    (fn []
      (let [current-state @state
            amount-to-heal (when-let [dice-totals (->> current-state
                                                       :values
                                                       vals
                                                       (mapcat vals)
                                                       seq)]
                             (let [dice-sum (apply + dice-totals)]
                               (when (> dice-sum 0)
                                 (let [con-mod (:con (<sub [::dnd5e/ability-modifiers]))
                                       dice-used (count dice-totals)]
                                   (+ (* dice-used con-mod)
                                      (:extra current-state)
                                      dice-sum)))))]
        [:div {:class (:short-rest-overlay styles)}
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
                                                    (count rolls)))
                                           {}
                                           (:values @state))]
                                 [::events/update-hp
                                  amount-to-heal
                                  (<sub [::dnd5e/max-hp])]
                                 [:toggle-overlay nil])}
          "Take a short rest"
          (when (> amount-to-heal 0)
            (str "; heal +" amount-to-heal))]]))))
