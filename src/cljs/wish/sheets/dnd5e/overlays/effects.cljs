(ns ^{:author "Daniel Leong"
      :doc "Effect-adder overlay"}
  wish.sheets.dnd5e.overlays.effects
  (:require [wish.sheets.dnd5e.style :as styles]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.util :refer [click>evts <sub]]
            [wish.views.widgets :as widgets]
            [wish.views.widgets.virtual-list :refer [virtual-list]]))

(defn- effect-browser-item [effect]
  [:<>
   [:div.name (:name effect)]
   [:div.add.button
    {:on-click (click>evts [:effect/add (:id effect)]
                           [:toggle-overlay nil]
                           [:5e/effects-filter ""])}
    "Add"]])

(defn overlay []
  [:div styles/item-adder-overlay
   [:h4 "Effects"]

   [widgets/search-bar
    {:filter-key :5e/effects-filter
     :placeholder "Search for an effect..."
     :auto-focus true}]

   [:div.item-browser.scrollable
    [virtual-list
     :items (<sub [::subs/all-effects])
     :render-item (fn [item]
                    [:div.item
                     [effect-browser-item item]])]]])
