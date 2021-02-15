(ns wish.sheets.dnd5e.overlays.allies
  "Ally management overlay"
  (:require [wish.sheets.dnd5e.overlays.style :as styles]
            [wish.sheets.dnd5e.subs.allies :as allies]
            [wish.util :refer [<sub]]
            [wish.views.widgets :as widgets]
            [wish.views.widgets.virtual-list :refer [virtual-list]]))

(defn- ally-browser-item [ally]
  [:div.ally
   [:div.name (:name ally)]
   ])

(defn overlay []
  [:div (styles/item-adder-overlay)
   [:h4 "Allies"]

   [widgets/search-bar
    {:filter-key :5e/allies-filter
     :placeholder "Search for an ally..."
     :auto-focus true}]

   [:div.item-browser.scrollable
    [virtual-list
     :items (<sub [::allies/all])
     :render-item (fn [item]
                    [:div.item
                     [ally-browser-item item]])]]])
