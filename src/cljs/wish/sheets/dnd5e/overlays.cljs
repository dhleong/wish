(ns ^{:author "Daniel Leong"
      :doc "Overlays"}
  wish.sheets.dnd5e.overlays
  (:require-macros [wish.util :refer [fn-click]]
                   [wish.util.log :as log :refer [log]])
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.inventory :as inv]
            [wish.sheets.dnd5e.data :as data]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.style :as styles]
            [wish.sheets.dnd5e.util :refer [->die-use-kw mod->str]]
            [wish.sheets.dnd5e.widgets :refer [spell-aoe spell-card
                                               spell-tags]]
            [wish.util :refer [<sub >evt click>evt click>evts click>swap!
                               dec-dissoc toggle-in]]
            [wish.views.util :refer [dispatch-change-from-keyup]]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
             :refer [expandable formatted-text link]]
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
        [formatted-text :div.desc d])]) ])


; ======= ability info/tmp mods ============================

(defn ability-tmp
  [id label]
  (let [{{:keys [score modifier]} id} (<sub [::subs/ability-info])]
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
        :save! (fn [path v]
                 (>evt [:update-meta
                        [:sheet :ability-tmp]
                        assoc id v]))}]]]))


; ======= hit points =======================================

(defn- condition-widget
  [[id level] on-delete]
  (let [c (get data/conditions id)]
    [:div.condition
     [expandable
      [:<>
       [:div.name
        [:a.delete
         {:href "#"
          :on-click (click>evt [:update-meta
                                [:sheet :conditions]
                                dec-dissoc
                                id]
                               :propagate? false)}
         (if (> level 1)
           (icon :remove-circle)
           (icon :clear))]
        (:name c)
        (when (:levels c)
          [:span.meta " (" level ")"])]
       (when-let [per-level (:per-level c)]
         [:ul.per-levels
          (for [the-level (range 1 (inc level))]
            ^{:key the-level}
            [:li (get per-level the-level)])])]

      [formatted-text :div.desc (:desc c)]]]))

(defn- conditions-management []
  (let [afflicted (<sub [::subs/conditions])]
    [:<>
     [:h5.centered.section-header "Conditions"]
     (for [[id _ :as c] afflicted]
       ^{:key id}
       [condition-widget c])

     [:div.centered

      (when-not (seq afflicted)
        [:div.none "(none)"])

      [:select
       {:id :condition
        :value :-none
        :on-change (fn-click [e]
                     (let [v (keyword (.. e -target -value))]
                       (when-not (= :-none v)
                         (set! (.. e -target -value) "-none")
                         (>evt [:update-meta [:sheet :conditions v] inc]))))}

       [:option {:key :-none}
        "— Add a Condition —"]

       (for [c (->> (data/conditions-sorted)
                    (filter (fn [{:keys [id levels]}]
                              (let [max-level (or levels 1)
                                    current-level (get afflicted id 0)]
                                (< current-level max-level)))))]
         ^{:key (:id c)}
         [:option {:value (:id c)}
          (:name c)])]
      ]]))

(defn hp-overlay []
  (let [[starting-hp _] (<sub [::subs/hp])
        state (r/atom {})]
    (fn []
      (let [[hp max-hp] (<sub [::subs/hp])
            {:keys [heal damage]} @state
            new-hp (max
                     0  ; you can't go negative in 5e
                     (min max-hp
                          (- (+ hp heal)
                             damage)))
            death-saves (<sub [::subs/death-saving-throws])]
        [:div styles/hp-overlay
         (when (= 0 hp)
           [:<>
            [:h4 "Death Saving Throws"]
            [:div.sections.spread.centered

             [:div "Failures"
              [widgets/slot-use-block
               {:total 3
                :used (:fails death-saves)
                :consume-evt [::events/update-death-saves inc :fails]
                :restore-evt [::events/update-death-saves dec :fails]}]]

             [:div "Successes"
              [widgets/slot-use-block
               {:total 3
                :used (:saves death-saves)
                :consume-evt [::events/update-death-saves inc :saves]
                :restore-evt [::events/update-death-saves dec :saves]}]]]])

         [:h4 "Hit Points"]
         [:div.sections
          [:a {:href "#"
               :on-click (click>evt [::events/update-hp -1 max-hp])}
           (icon :remove-circle)]

          [:div.current-hp hp]

          [:a {:href "#"
               :on-click (click>evt [::events/update-hp 1 max-hp])}
           (icon :add-circle)]]

         [:h5.centered.section-header "Quick Adjust"]
         [:form#hp-adjust-input
          {:on-submit (fn-click
                        (let [{:keys [heal damage]} @state]
                          (log "Update HP: heal +" heal "  -" damage)
                          (>evt [::events/update-hp (- heal damage) max-hp])
                          (>evt [:toggle-overlay nil])))}
          [:div.sections

           [:div.quick-adjust

            ; left col: damage
            [:div "Damage"]
            [bind-fields
             [:input.number {:field :fast-numeric
                             :id :damage
                             :min 0}]
             {:get #(get-in @state %)
              :save! #(swap! state (fn [s]
                                     (-> s
                                         (assoc-in %1 %2)
                                         (dissoc :heal))))}]]

           ; new HP in the middle
           [:div.new-hp
            (when (not= new-hp hp)
              [:<>
               [:div.label "New HP"]
               [:div.amount
                {:class (if (> new-hp hp)
                          "healing"
                          "damage")}
                new-hp] ])]

           ; right col: heal
           [:div.quick-adjust
            [:div "Heal"]

            [bind-fields
             [:input.number {:field :fast-numeric
                             :id :heal
                             :min 0}]
             {:get #(get-in @state %)
              :save! #(swap! state (fn [s]
                                     (-> s
                                         (assoc-in %1 %2)
                                         (dissoc :damage))))}]]]
          (when (not= new-hp hp)
            [:div.sections
             [:input.apply {:type 'submit
                            :value "Apply!"}] ])]

         ; temporary health management
         [:h5.centered.section-header "Temporary Health"]
         [:div.sections
          [:div.quick-adjust
           [:div "Temp HP"]

           [bind-fields
            [:input.number {:field :fast-numeric
                            :id :temp-hp
                            :min 0}]
            {:get #(<sub [::subs/temp-hp])
             :save! #(>evt [::events/temp-hp! %2])}]]

          ; just a spacer
          [:div.new-hp]

          [:div.quick-adjust
           [:div "Extra Max HP"]

           [bind-fields
            [:input.number {:field :fast-numeric
                            :id :temp-max-hp
                            :min 0}]
            {:get #(<sub [::subs/temp-max-hp])
             :save! #(>evt [::events/temp-max-hp! %2])}]]]

          ; this ought to get its own overlay at some point:
          [conditions-management]]))))


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
  (let [dice-info (<sub [::subs/hit-dice])
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
  (let [con-mod (-> (<sub [::subs/ability-modifiers])
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
                                 (let [con-mod (:con (<sub [::subs/ability-modifiers]))
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
                                  (<sub [::subs/max-hp])]
                                 [:toggle-overlay nil])}
          "Take a short rest"
          (when (> amount-to-heal 0)
            (str "; heal +" amount-to-heal))]]))))


; ======= Spell management =================================

(defn- spell-info-header [opts s]
  [:div.info opts
   [:div.name (:name s)]
   [:div.meta
    [:span.level
     (if (= 0 (:spell-level s))
       "Cantrip"
       (str "Level " (:spell-level s)))]
    [spell-tags s]]] )

(defn- spell-block
  [s {:keys [selectable?
             source-list
             verb]}]

  (let [expanded? (r/atom false)]
    (fn [s {:keys [selectable?
                   source-list
                   verb]}]
      [:div.spell
       [:div.header
        [spell-info-header
         {:on-click (click>swap! expanded? not)}
         s]
        (if (:always-prepared? s)
          [:div.prepare.disabled
           {:title "Always Prepared"}
           (icon :check-circle-outline)]

          [:div.prepare
           {:class (when-not (or (:prepared? s)
                                 (selectable? s))
                     "disabled")
            :on-click (click>evt [:update-option-set source-list
                                  (if (:prepared? s)
                                    disj
                                    conj)
                                  (:id s)])}
           (if (:prepared? s)
             (icon :check-circle)
             verb)])]

       (when @expanded?
         [spell-card s])])))

(defn spell-management
  [the-class & {:keys [mode]
                :or {mode :default}}]
  (let [attrs (-> the-class :attrs :5e/spellcaster)
        {:keys [acquires? prepares?]} attrs

        knowable (<sub [::subs/knowable-spell-counts (:id the-class)])

        ; in :acquisition mode (eg: for spellbooks), cantrips have
        ; the normal limit but spells are unlimited
        limits (case mode
                 :default knowable
                 :acquisition (dissoc knowable :spells))

        prepare-verb (cond
                       ; TODO we could add an :acquire-verb...
                       (= :acquisition mode) "Acquire"
                       prepares? "Prepare"
                       :else "Learn")

        title (case mode
                :default (str "Manage "
                              (:name the-class)
                              (if prepares?
                                " Prepared"
                                " Known")
                              " Spells")
                :acquisition (str "Manage "
                                  (:name the-class)
                                  " "
                                  (:acquired-label attrs)))

        spells-limit (:spells limits)
        cantrips-limit (when-not (and acquires?
                                      (= :default mode))
                         (:cantrips limits))

        available-list (if (and
                             acquires?
                             (not= :acquisition mode))
                         ; for an :acquires? spellcaster in default mode,
                         ; the source for their prepared spells is their
                         ; :acquires?-spells list
                         (:acquires?-spells attrs)

                         ; otherwise, it's the :spells list
                         (:spells attrs))

        all-prepared (<sub [::subs/my-prepared-spells-by-type (:id the-class)])
        prepared-spells-count (count (:spells all-prepared))
        prepared-cantrips-count (count (:cantrips all-prepared))

        spells (<sub [::subs/preparable-spell-list the-class available-list])

        can-select-spells? (or (nil? spells-limit)
                               (< prepared-spells-count spells-limit))
        can-select-cantrips? (< prepared-cantrips-count cantrips-limit)
        spell-opts (assoc attrs
                          :verb prepare-verb
                          :source-list available-list
                          :selectable? (fn [{:keys [spell-level]}]
                                         (if (= 0 spell-level)
                                           can-select-cantrips?
                                           can-select-spells?)))]

    [:div styles/spell-management-overlay
     [:h5 title
      (when spells-limit
        [:div.limit
         "Spells " prepared-spells-count " / " spells-limit])
      (when cantrips-limit
        [:div.limit
         "Cantrips " prepared-cantrips-count " / " cantrips-limit])]

     [widgets/search-bar
      {:filter-key :5e/spells-filter
       :placeholder "Search for a spell..."}]

     #_[:div.stretch
      [virtual-list
       :items spells
       :render-item (fn [opts item]
                      [:div.spell-container opts
                       [spell-block item spell-opts]])]]
     (for [s spells]
       ^{:key (:id s)}
       [spell-block s spell-opts])]))


; ======= spell info =======================================

(defn spell-info [s]
  [:div styles/spell-info-overlay
   [spell-info-header {} s]
   [spell-card s]])


; ======= currency =========================================

(defn currency-manager []
  (let [quick-adjust (r/atom {})]
    (fn []
      [bind-fields
       [:form
        {:on-submit (fn-click
                      (when-let [v (:adjust @quick-adjust)]
                        (log "Adjust currency: " v)
                        (>evt [::events/adjust-currency v]))
                      (>evt [:toggle-overlay nil]))}
        [:input {:type 'submit
                 :style {:display 'none}}]
        [:div styles/currency-manager-overlay
         [:h5 "Currency"]
         [:table
          [:tbody
           [:tr
            [:th.header.p "Platinum"]
            [:th.header.g "Gold"]
            [:th.header.e "Electrum"]
            [:th.header.s "Silver"]
            [:th.header.c "Copper"]]

           ; current values
           [:tr
            [:td
             [:input.amount {:field :fast-numeric
                             :id :platinum}]]
            [:td
             [:input.amount {:field :fast-numeric
                             :id :gold}]]
            [:td
             [:input.amount {:field :fast-numeric
                             :id :electrum}]]
            [:td
             [:input.amount {:field :fast-numeric
                             :id :silver}]]
            [:td
             [:input.amount {:field :fast-numeric
                             :id :copper}]]]

           [:tr
            [:td.meta {:col-span 5}
             "Adjust totals directly"]]

           [:tr
            [:th {:col-span 5
                  :style {:padding-top "1em"}}
             "Quick Adjust"]]

           [:tr
            [:td
             [:input.amount {:field :fast-numeric
                             :id :adjust.platinum}]]
            [:td
             [:input.amount {:field :fast-numeric
                             :id :adjust.gold}]]
            [:td
             [:input.amount {:field :fast-numeric
                             :id :adjust.electrum}]]
            [:td
             [:input.amount {:field :fast-numeric
                             :id :adjust.silver}]]
            [:td
             [:input.amount {:field :fast-numeric
                             :id :adjust.copper}]]]

           [:tr
            [:td.meta {:col-span 5}
             "Add or subtract amounts by inputting above and pressing 'enter'"]]

           ]]]]

       {:get #(if (= :adjust (first %))
                (get-in @quick-adjust %)
                (get-in (<sub [::subs/currency]) %))

        :save! (fn [path v]
                 (if (not= :adjust (first path))
                   (>evt [::events/set-currency (first path) v])

                   (swap! quick-adjust assoc-in path v)))}])))


; ======= custom item creation =============================

; NOTE public for testing!
(defn install-limited-use
  "Modify the custom item spec to install the limited use directives
   if necessary, and remove any related keys as necessary"
  [{:keys [limited-use?]
    item-id :id
    item-name :name
    {use-name :name
     uses :uses} :limited-use
    :as item}]
  (let [item (if limited-use?
               (assoc item
                      :! [[:!add-limited-use
                           {:id item-id
                            :name (or use-name item-name)
                            :uses (or uses 1)
                            :restore-trigger :long-rest}]])

               ; strip :attunes? if there's no limited-use
               (dissoc item :attunes?))]

    ; always strip these keys
    (dissoc item :limited-use? :limited-use)))

(defn custom-item-creator []
  (let [state (r/atom {:type :other})

        ; creates a :visible? fn that returns true when
        ; the selected :type is equal to the provided key
        for-type (fn [type]
                   (comp (partial = type) :type))

        ; as above, but for any of the provided types
        for-types (fn [& types]
                    (comp (set types) :type))

        ; function that returns a section for selecting the :kind
        ; of the current item type
        kind-selector (fn [type values-fn]
                        [:div.section {:field :container
                                       :visible? (for-type type)}
                         [:label {:for [:kind type]}
                          "Kind\u00A0"]
                         [:select {:field :list
                                   :id [:kind type]}
                          (for [{:keys [id label]} (values-fn)]
                            [:option {:key id}
                             label])]])

        ; create the item once everything is validated
        create! (fn [s]
                  (let [custom-id (inv/custom-id
                                    (:name s))
                        s (-> s
                              (dissoc :errors)
                              (assoc :id custom-id)

                              ; create any requested limited-use directive
                              install-limited-use

                              ; pull up the appropriate kind
                              (assoc :kind (get-in s [:kind (:type s)]))

                              ; and inflate
                              data/inflate-by-type)]

                    (log "Add! custom item" s)
                    (>evt [:inventory-add s])
                    (>evt [:toggle-overlay nil])))]

    (fn []
      [:div styles/custom-item-overlay
       [:h5 "Custom Item"]
       [bind-fields
        [:form
         {:on-submit (fn-click
                       (let [s @state]
                         (cond
                           (str/blank? (:name s))
                           (swap! state assoc-in [:errors :name]
                                  "Name must not be blank")

                           (and (= :ammunition (:type s))
                                (or (str/blank? (:default-quantity s))
                                    (<= (:default-quantity s)
                                        0)))
                           (swap! state assoc-in [:errors :default-quantity]
                                  "You must provide a quantity > 0")

                           :else (create! s))))}

         [:div.section
          [:input {:field :text
                   :id :name
                   :placeholder "Name"
                   :auto-focus true
                   :auto-complete "off"}]
          [:div.error {:field :alert
                       :id :errors.name}]]

         [:div.section
          [:label {:for :type}
           "Type\u00A0"]
          [:select {:field :list
                    :id :type}
           [:option {:key :ammunition} "Ammunition"]
           [:option {:key :armor} "Armor"]
           [:option {:key :gear} "Gear"]
           [:option {:key :weapon} "Weapon"]
           [:option {:key :potion} "Potion"]
           [:option {:key :other} "Other"]]
          ]

         ; ammunition config
         [:div.section {:field :container
                        :visible? (for-type :ammunition)}
          [:input {:field :fast-numeric
                   :id :default-quantity
                   :placeholder "Default Quantity"}]
          [:div.error {:field :alert
                       :id :errors.default-quantity}]]

         ; armor config
         (kind-selector :armor data/armor-kinds)

         ; weapon config
         (kind-selector :weapon data/weapon-kinds)

         [:div.section.flex
          [:div {:field :container
                 :visible? (for-types :ammunition
                                      :weapon
                                      :armor)}
           [:label {:for :+}
            "+\u00A0"]
           [:input.numeric {:field :fast-numeric
                            :id :+}]]

          [:div {:field :container
                 :visible? (for-types :gear
                                      :weapon
                                      :armor)}
           [:input {:field :checkbox
                    :id :limited-use?}]
           [:label.meta {:for :limited-use?}
            "Provides a Limited-Use"]]]

         [:div.section.limited-use {:field :container
                                    :visible? #(:limited-use? %)}
          [:div
           [:input {:field :checkbox
                    :id :attunes?}]
           [:label.meta {:for :attunes?}
            "Requires attunement"]]

          [:div
           [:input {:field :text
                    :id :limited-use.name
                    :placeholder "Limited Use Name"}]]

          [:div
           [:label.meta {:for :attunes?}
            "Uses\u00A0"]
           [:input.numeric {:field :fast-numeric
                            :id :limited-use.uses}]]]

         [:div.section
          [:textarea.stretch {:field :textarea
                              :id :desc
                              :placeholder "Description (optional)"
                              :rows 4}]]

         [:div.section
          [:input {:type :submit
                   :value "Create!"}]]
         ]

        ; the state atom
        state

        ; events:
        (fn [id value doc]
          (when (and (some (set id) [:name
                                     :default-quantity])
                     (not (str/blank? value)))
            (update doc :errors dissoc (first id))))]])))


; ======= starting equipment ==============================

(defn- direct-click?
  "Return true if the click event was directly on the
   desired element"
  [e]
  ; okay, sort of; this is slightly simpler than having
  ; to track the actual dom elements
  (not (#{"SELECT"}
         (.. e -target -tagName))))

(defn- equipment-choice
  [state path choices enabled?]
  [:select
   {:on-change (fn-click [e]
                 (swap! state
                        assoc-in path
                        (int (.. e -target -value))))
    :value (if-let [v (get-in @state path)]
             v
             js/undefined)
    :disabled (not enabled?)}
   (for [[i c] (map-indexed list choices)]
     ^{:key (:id c)}
     [:option {:value i}
      (:name c)])])

(defn- equipment-count
  [item amount]
  [:span [:b "(" amount ") "] (:name item)])

(defn- equipment-pack
  ([pack]
   (equipment-pack pack false))
  ([pack expanded?]
   [:div.pack
    [:div.name (:name pack)]
    (when expanded?
      [:div.contents
       (->> pack
            :contents
            (map (fn [[item amount]]
                   (str amount " " (:name item))))
            (str/join ", "))])]))

(defn- equipment-and
  [state path values chosen?]
  (let [top-level? (= 1 (count path))
        chosen-path (conj path :chosen)
        options-count (count values)]
    [:div
     (when top-level?
       {:class "alternatives clickable"
        :on-click (fn-click [e]
                    (when (direct-click? e)
                      (swap! state toggle-in chosen-path true)))})
     [:div
      {:class (when top-level?
                ["choice" (when chosen?
                            "chosen")])}
      (for [[i v] (map-indexed list values)]
        ^{:key i}
        [:span
         (cond
           (= i 0) nil
           (= i (dec options-count)) " and "
           :else ", ")
         (cond
           (and (vector? v)
                (= :or (first v)))
           [equipment-choice state (conj path i) (second v) chosen?]

           (and (vector? v)
                (= :count (first v)))
           [equipment-count (second v) (last v)]

           ; single item
           :else
           (:name v))])]]))

(defn- equipment-or
  [state path choices]
  (let [chosen-path (conj path :chosen)
        chosen (get-in @state chosen-path)]
    [:div.alternatives
    (for [[i v] (map-indexed list choices)]
      (let [chosen? (= chosen i)]
        ^{:key i}
        [:div.choice.clickable
         {:on-click (fn-click [e]
                      ; ignore clicks on contained, clickable children (esp [select])
                      (when (direct-click? e)
                        (swap! state toggle-in chosen-path i)))
          :class (when chosen?
                   "chosen")}
         (if (vector? v)
           (let [[kind v amount] v]
             ; special case
             (case kind
               :count [equipment-count v amount]
               :pack [equipment-pack v (when chosen?
                                         :expand!)]
               :and [equipment-and state (conj path i) v chosen?]
               :or (if chosen?
                     [equipment-choice state (conj path i) v :enabled]
                     [:span "(choice)"])))

           ; single item
           [:span (:name v)])]))]))

(defn expand-starting-eq
  ([choices state-map]
   (->> choices
        (map-indexed list)
        (mapcat
          (fn [[i outer-choice]]
            (expand-starting-eq outer-choice state-map [i] false)))))

  ([choice state-map path and?]
   (when-let [chosen (or (get-in state-map (conj path :chosen))
                         (get-in state-map path)
                         and?)]
     (if (vector? choice)
       (let [[kind values ?amount] choice]
         (case kind
           ; wacky (when-not) to handle de-selected top-level and
           :and (when-not (and (map? chosen)
                               (contains? chosen :chosen)
                               (not (:chosen chosen)))
                  (->> values
                       (map-indexed list)
                       (mapcat
                         (fn [[i item]]
                           (expand-starting-eq item state-map
                                               (conj path i)
                                               :and!)))))

           :or (when-let [chosen (if (number? chosen)
                                   chosen

                                   ; top-level :or do NOT have a default
                                   ; value, but nested ones do
                                   (when (> (count path) 1)
                                     0))]
                 (expand-starting-eq
                   (nth values chosen)
                   state-map
                   (conj path chosen)
                   true))

           ; easy peasy
           :count [[values ?amount]]

           ; also easy
           :pack (-> values :contents)))

       ; simple case
       [choice]))))

(defn starting-equipment-adder []
  (let [state (r/atom {})]
    (fn []
      (let [{primary-class :class
             choices :choices
             :as info} (<sub [::subs/starting-eq])
            this-state @state]
        [:div styles/starting-equipment-overlay
         [:h5 (:name primary-class) " Starting Equipment"]

         (for [[i [kind values]] (map-indexed list choices)]
           (with-meta
             (case kind
               :or [equipment-or state [i] values]
               :and [equipment-and state [i] values
                     (when (get this-state i)
                       :chosen!)])
             {:key i}))

         (when (some :chosen (vals this-state))
           [:div.accept
            [:a {:href "#"
                 :on-click (fn-click
                             (let [items (expand-starting-eq
                                           choices
                                           @state)]
                               (log "State:" @state)
                               (log "Add items: " items)
                               (>evt [:inventory-add-n items])
                               (>evt [:toggle-overlay nil])))}
             "I'll take it!"]])]))))
