(ns ^{:author "Daniel Leong"
      :doc "Overlays"}
  wish.sheets.dnd5e.overlays
  (:require [clojure.string :as str]
            [reagent-forms.core :refer [bind-fields]]
            [wish.inventory :as inv]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.overlays.spell-management
             :refer [spell-info-header]]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.subs.abilities :as abilities]
            [wish.sheets.dnd5e.subs.inventory :as inventory]
            [wish.sheets.dnd5e.overlays.style :as styles]
            [wish.sheets.dnd5e.widgets :refer [item-quantity-manager
                                               spell-aoe
                                               spell-card]]
            [wish.util :refer [<sub >evt click>evt click>evts]]
            [wish.views.widgets :as widgets
             :refer [formatted-text]]
            [wish.views.widgets.fast-numeric]
            [wish.views.widgets.virtual-list :refer [virtual-list]]))

; ======= generic "info" overlay ==========================

(def ^:private properties
  {:finesse? "Finesse"
   :heavy? "Heavy"
   :light? "Light"
   :reach? "Reach"
   :special? "Special"
   :two-handed? "Two-handed"
   :uses-ammunition? "Uses Ammunition"
   :versatile "Versatile"})

(defn- generic-info [entity]
  (let [{:keys [aoe damage dice range]} entity]
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
                  (dice (:wish/container entity))
                  dice)]])
        ]]
      )))

(defn info [entity]
  [:div (styles/info-overlay)
   (when-let [n (:name entity)]
     [:div.name n])

   (if (:spell-level entity)
     [spell-card entity]

     [:<>
      (generic-info entity)

      (when-let [d (:desc entity)]
        [formatted-text :div.desc d])

      (when-let [effects (seq (:effects entity))]
        [:ul
         (for [effect effects]
           ^{:key effect}
           [:li effect])])

      (when (inv/stacks? entity)
        [item-quantity-manager entity])]) ])


; ======= effects =========================================

(defn effect-info [entity]
  [:div (styles/info-overlay)
   [:div.name (:name entity)]

   (generic-info entity)

   (let [{:keys [spell-level duration]} entity]
     (when (or spell-level duration)
       [:table.info
        [:tbody
         (when spell-level
           [:tr
            [:th.header "Spell Level"]
            [:td spell-level]])

         (when duration
           [:tr
            [:th.header "Duration"]
            [:td duration]])]]))

   (when-let [d (:desc entity)]
     [formatted-text :div.desc d])

   (when-let [effects (seq (:effects entity))]
     [:ul
      (for [effect effects]
        ^{:key effect}
        [:li effect])])

   [:div.button {:on-click (click>evts
                             [:effect/remove (:id entity)]
                             [:toggle-overlay nil])}
    "End Effect"]])


; ======= ability info/tmp mods ============================

(defn ability-tmp
  [id label]
  (let [{{:keys [score modifier]} id} (<sub [::abilities/info])]
    [:div (styles/ability-tmp-overlay)
     [:h5 label " " score " (" modifier ")"]

     [:form
      {:on-submit (click>evt [:toggle-overlay nil])}

      [bind-fields

       [:div
        [:label {:for :ability-tmp}
         "Temporary Modifier"]
        [:input.number {:field :fast-numeric
                        :id :ability-tmp}]]

       {:get #(get-in (<sub [:meta/sheet]) [:ability-tmp id])
        :save! (fn [_path v]
                 (>evt [:update-meta
                        [:sheet :ability-tmp]
                        assoc id v]))}]]]))



; ======= notes ============================================

(defn- actual-notes-overlay
  [bind-opts]
  [:div (styles/notes-overlay)
   [:h5 "Notes"]
   [bind-fields
    [:textarea.notes {:field :textarea
                      :id :notes}]
    bind-opts]])

(defn notes-overlay
  ([]
   (actual-notes-overlay
     {:get #(<sub [::subs/notes])
      :save! #(>evt [::events/set-notes %2])}))
  ([kind entity]
   (case kind
     :item (actual-notes-overlay
             {:get #(get-in (<sub [:inventory-map])
                            [(:id entity) :notes])
              :save! #(>evt [:update-meta [:items (:id entity)]
                             assoc
                             :notes
                             %2])}))))

; ======= long rest =======================================

(defn long-rest-overlay []
  [:div (styles/short-rest-overlay)
   [:h5 "Long Rest"]

   ; SRD description:
   [:p.desc "A long rest is a period of extended downtime, at least 8 hours long, during which a character sleeps or performs light activity: reading, talking, eating, or standing watch for no more than 2 hours. If the rest is interrupted by a period of strenuous activity—at least 1 hour of walking, fighting, casting spells, or similar adventuring activity—the characters must begin the rest again to gain any benefit from it."]

   [:div.button
    {:on-click (click>evts [:trigger-limited-use-restore
                            [:short-rest :long-rest]]
                           [:toggle-overlay nil])}
    "Take a long rest" ]])


; ======= spell info =======================================

(defn spell-info [s]
  [:div (styles/spell-info-overlay)
   [spell-info-header {} s]
   [spell-card s]])


; ======= item adder ======================================

(defn- item-browser-item [item]
  [:<>
   [:div.name (:name item)]
   [:div.add.button
    {:on-click (click>evts [:inventory-add item]
                           [:toggle-overlay nil]
                           [:5e/items-filter ""])}
    "Add"]])

(defn- item-browser []
  [:<>
   [widgets/search-bar
    {:filter-key :5e/items-filter
     :placeholder "Search for an item..."
     :auto-focus true}]

   [:div.item-browser.scrollable
    [virtual-list
     :items (<sub [::inventory/all-items])
     :render-item (fn [item]
                    [:div.item
                     [item-browser-item item]])]]])

(defn item-adder []
  [:div (styles/item-adder-overlay)
   [:h4 "Add Items"]

   [item-browser]
   ])

