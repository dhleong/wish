(ns wish.sheets.dnd5e.overlays.allies
  "Ally management overlay"
  (:require [spade.core :refer [defattrs]]
            [wish.inventory :refer [instantiate-id]]
            [wish.sheets.dnd5e.overlays.style :as styles]
            [wish.sheets.dnd5e.subs.allies :as allies]
            [wish.util :refer [click>evts <sub]]
            [wish.views.widgets :as widgets]
            [wish.views.widgets.virtual-list :refer [virtual-list]]))

(defattrs ally-attrs []
  {:display :flex
   :width :100%
   :align-items :center
   :flex-direction :row}
  [:.name {:flex-grow 1}])

(defn- ally-browser-item [{id :id :as ally}]
  [:div (ally-attrs)
   [:div.name (:name ally)]

   [:div.button {:on-click (click>evts
                             [:ally/add {:id id
                                         :instance-id (instantiate-id id)}]
                             [:toggle-overlay nil]
                             [:5e/allies-filter ""])}
    "Summon"]
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
