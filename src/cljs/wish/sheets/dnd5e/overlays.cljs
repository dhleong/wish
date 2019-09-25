(ns ^{:author "Daniel Leong"
      :doc "Overlays"}
  wish.sheets.dnd5e.overlays
  (:require-macros [wish.util :refer [fn-click]])
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.inventory :as inv]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.overlays.spell-management
             :refer [spell-info-header]]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.subs.abilities :as abilities]
            [wish.sheets.dnd5e.subs.hp :as hp]
            [wish.sheets.dnd5e.subs.inventory :as inventory]
            [wish.sheets.dnd5e.style :as styles]
            [wish.sheets.dnd5e.util :refer [->die-use-kw mod->str]]
            [wish.sheets.dnd5e.widgets :refer [item-quantity-manager
                                               spell-aoe
                                               spell-card]]
            [wish.util :refer [<sub >evt click>evt click>evts click>swap!]]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
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

(defn- generic-info
  [entity]
  (let [{:keys [aoe damage range]} entity]
    (when (or aoe damage range)
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
        ]]
      )))

(defn info
  [entity]
  [:div styles/info-overlay
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
  [:div styles/info-overlay
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
    [:div styles/ability-tmp-overlay
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
  [:div styles/notes-overlay
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


; ======= short rest =======================================

(defn dice-pool [state]
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
           :on-click (click>swap! state #(update-in % [:values die] dissoc i))}
       (icon :remove-circle)])]

   state])

(defn dice-usage [state]
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

(defn short-rest-overlay []
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
      [:div styles/short-rest-overlay
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


; ======= long rest =======================================

(defn long-rest-overlay []
  [:div styles/short-rest-overlay
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
  [:div styles/spell-info-overlay
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
  [:div styles/item-adder-overlay
   [:h4 "Add Items"]

   [item-browser]
   ])

