(ns ^{:author "Daniel Leong"
      :doc "Effect-adder overlay"}
  wish.sheets.dnd5e.overlays.effects
  (:require [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.sheets.dnd5e.style :as styles]
            [wish.sheets.dnd5e.subs.effects :as effects]
            [wish.util :refer [click>evts <sub]]
            [wish.views.widgets :as widgets]
            [wish.views.widgets.virtual-list :refer [virtual-list]]))

(defn- add-effect-button
  ([effect] (add-effect-button effect true))
  ([effect arg]
   [:div.add.button
    {:on-click (click>evts [:effect/add (:id effect) arg]
                           [:toggle-overlay nil]
                           [:5e/effects-filter ""])}
    "Add"]))

(defn- spell-effect-applier [{:keys [from-spell] :as effect}]
  (r/with-let [[min-level max-level] from-spell
               state (r/atom {:spell-level min-level})]
    [:<>

     [bind-fields
      [:select.level {:field :list
                      :id [:spell-level]}
       (for [level (range min-level (inc max-level))]
         [:option
          {:key level}
          (str "Level " level)])]

      state]

     [add-effect-button effect @state]]))

(defn- effect-browser-item [effect]
  [:<>
   [:div.name (:name effect)]

   (cond
     (:from-spell effect) [spell-effect-applier effect]
     :else [add-effect-button effect])])

(defn overlay []
  [:div (styles/item-adder-overlay)
   [:h4 "Effects"]

   [widgets/search-bar
    {:filter-key :5e/effects-filter
     :placeholder "Search for an effect..."
     :auto-focus true}]

   [:div.item-browser.scrollable
    [virtual-list
     :items (<sub [::effects/all])
     :render-item (fn [item]
                    [:div.item
                     [effect-browser-item item]])]]])
