(ns ^{:author "Daniel Leong"
      :doc "Overlays"}
  wish.sheets.dnd5e.overlays
  (:require-macros [wish.util.log :refer [log]])
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.subs :as dnd5e]
            [wish.sheets.dnd5e.style :refer [styles]]
            [wish.sheets.dnd5e.util :refer [->die-use-kw mod->str]]
            [wish.util :refer [<sub >evt click>evt click>evts]]
            [wish.views.util :refer [dispatch-change-from-keyup]]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
             :refer [formatted-text link]]
            [wish.views.widgets.fast-numeric]))

; ======= hit points =======================================

(defn hp-overlay []
  (let [[starting-hp _] (<sub [::dnd5e/hp])
        state (r/atom {})]
    (fn []
      (let [[hp max-hp] (<sub [::dnd5e/hp])
            {:keys [heal damage]} @state
            new-hp (max
                     0  ; you can't go negative in 5e
                     (min max-hp
                          (- (+ hp heal)
                             damage)))
            death-saves (<sub [::dnd5e/death-saving-throws])]
        [:div {:class (:hp-overlay styles)}
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

          ; TODO support for tmp hp, temporarily modified max hp,
          ; boxes to input damage/healing, and possibly a "deferred update"
          ; mechanism somehow.
          [:div.current-hp hp]

          [:a {:href "#"
               :on-click (click>evt [::events/update-hp 1 max-hp])}
           (icon :add-circle)]]

         [:h5.centered "Quick Adjust"]
         [:form#hp-adjust-input
          {:on-submit (fn [e]
                        (.preventDefault e)
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
           ]))))


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
                                       dice-used (->> dice-totals
                                                      (keep identity)
                                                      count)]
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
                                             (println "ROLLS" rolls)
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
                                  (<sub [::dnd5e/max-hp])]
                                 [:toggle-overlay nil])}
          "Take a short rest"
          (when (> amount-to-heal 0)
            (str "; heal +" amount-to-heal))]]))))


; ======= Spell management =================================

(defn- spell-block
  [s {:keys [source-list
             verb]}]
  [:div.spell
   [:div.name (:name s)]
   [:div.prepare
    {:on-click (click>evt [:update-option-set source-list
                           (if (:prepared? s)
                             disj
                             conj)
                           (:id s)])}
    (if (:prepared? s)
      (icon :check-circle)
      verb)]])

(defn spell-management
  [the-class & {:keys [mode]
                :or {mode :default}}]
  (let [attrs (-> the-class :attrs :5e/spellcaster)
        {:keys [acquires? prepares?]} attrs

        knowable (<sub [::dnd5e/knowable-spell-counts (:id the-class)])

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
        cantrips-limit (:cantrips limits)

        available-list (if (and
                             acquires?
                             (not= :acquisition mode))
                         ; for an :acquires? spellcaster in default mode,
                         ; the source for their prepared spells is their
                         ; :acquires?-spells list
                         (:acquires?-spells attrs)

                         ; otherwise, it's the :spells list
                         (:spells attrs))

        all-prepared (<sub [::dnd5e/prepared-spells-by-type (:id the-class)])
        prepared-spells (:spells all-prepared)
        prepared-cantrips (:cantrips all-prepared)

        spells (<sub [::dnd5e/preparable-spell-list the-class available-list])

        spell-opts (assoc attrs
                          :verb prepare-verb
                          :source-list available-list)]

    [:div {:class (:spell-management-overlay styles)}
     [:h5 title
      (when spells-limit
        [:div.limit
         "Spells " (count prepared-spells) " / " spells-limit])
      [:div.limit
       "Cantrips " (count prepared-cantrips) " / " cantrips-limit]]

     (for [s spells]
       ^{:key (:id s)}
       [spell-block s spell-opts]) ]))
