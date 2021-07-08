(ns wish.sheets.dnd5e.overlays.allies
  "Ally management overlay"
  (:require [santiago.select :refer [select]]
            [spade.core :refer [defattrs]]
            [wish.inventory :refer [instantiate-id]]
            [wish.sheets.dnd5e.overlays.style :as styles]
            [wish.sheets.dnd5e.subs.allies :as allies]
            [wish.style.flex :as flex]
            [wish.util :refer [click>evt click>evts <sub >evt]]
            [wish.views.widgets :as widgets :refer-macros [icon]]
            [wish.views.widgets.virtual-list :refer [virtual-list]]))

(defattrs challenge-indicator-attrs []
  (merge flex/vertical
         flex/center
         {:padding-right "8px"})
  [:.label {:font-size "80%"}])

(defn- challenge-indicator [rating]
  [:div (challenge-indicator-attrs)
   [:div.label "CR"]
   [:div.value (case rating
                 0.125 "⅛"
                 0.25 "¼"
                 0.5 "½"
                 rating)]])

(defattrs ally-attrs []
  {:display :flex
   :width :100%
   :align-items :center
   :flex-direction :row}
  [:.favorite {:padding "4px"}]
  [:.name {:flex-grow 1}])

(defn- ally-browser-item [{:keys [id preferred?] :as ally}]
  [:div (ally-attrs)
   [:div.favorite
    (let [ico (if preferred?
                (icon :favorite)
                (icon :favorite-border))]
      (if (= :forced preferred?)
        ico
        [:a {:href "#"
             :on-click (click>evt [:ally/toggle-favorite ally])}
         ico]))]

   [challenge-indicator (:challenge ally)]

   [:div.name (:name ally)]

   [:div.button {:on-click (click>evts
                             [:ally/add {:id id
                                         :instance-id (instantiate-id id)}]
                             [:toggle-overlay nil]
                             [:5e/allies-filter ""])}
    "Summon"]
   ])

(defattrs ally-category-attrs []
  [:.desc {:font-size :80%
           :padding "4px"}])

(defn- ally-category-selector []
  (when-let [categories (seq (<sub [:allies/categories]))]
    (let [selected (<sub [:allies/selected-category])]
      [:div (ally-category-attrs)
       "Category Filter:"

       [select {:on-change (fn [new-id]
                             (>evt [:allies/select-category new-id]))
                :value (:id selected)}
        [:option {:key nil}
         "(None)"]

        (for [{:keys [id] :as category} categories]
          [:option {:key id}
           (:name category)])]

       (when selected
         [:div.desc (:desc selected)])
       ])
    ))

(defn overlay []
  [:div (styles/item-adder-overlay)
   [:h4 "Allies"]

   [widgets/search-bar
    {:filter-key :5e/allies-filter
     :placeholder "Search for an ally..."
     :auto-focus true}]

   [ally-category-selector]

   [:div.item-browser.scrollable
    [virtual-list
     :items (<sub [::allies/all])
     :render-item (fn [item]
                    [:div.item
                     [ally-browser-item item]])]]])
