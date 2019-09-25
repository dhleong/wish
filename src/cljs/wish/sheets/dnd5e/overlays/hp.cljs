(ns wish.sheets.dnd5e.overlays.hp
  (:require-macros [wish.util :refer [fn-click]]
                   [wish.util.log :as log :refer [log]])
  (:require [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.sheets.dnd5e.data :as data]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.subs.hp :as hp]
            [wish.sheets.dnd5e.style :as styles]
            [wish.util :refer [<sub >evt click>evt dec-dissoc]]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
             :refer [expandable formatted-text]]
            [wish.views.widgets.fast-numeric]))

(defn- condition-widget
  [[id level] _on-delete]
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

(defn- saving-throws []
  (let [death-saves (<sub [::hp/death-saving-throws])]
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
         :restore-evt [::events/update-death-saves dec :saves]}]]]]))

(defn- hp-form [& {:keys [hp max-hp]}]
  [:div.sections
   [:a {:href "#"
        :on-click (click>evt [::events/update-hp -1 max-hp])}
    (icon :remove-circle)]

   [:div.current-hp hp]

   [:a {:href "#"
        :on-click (click>evt [::events/update-hp 1 max-hp])}
    (icon :add-circle)]])

(defn- quick-adjust-form [state & {:keys [new-hp hp max-hp]}]
  [:form#hp-adjust-input
   {:on-submit (fn-click
                 (let [{:keys [heal damage]} @state]
                   (log "Update HP: heal +" heal "  -" damage)
                   (>evt [::events/update-hp (- heal damage) max-hp])
                   (>evt [:toggle-overlay nil])))}
   [:div.sections

    [:div.quick-adjust

     ; left col: damage
     [:div.label "Damage"]
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
     [:div.label "Heal"]

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
                     :value "Apply!"}] ])])

(defn- temp-hp-form []
  [:div.sections
   [:div.quick-adjust
    [:div.label "Temp HP"]

    [bind-fields
     [:input.number {:field :fast-numeric
                     :id :temp-hp
                     :min 0}]
     {:get #(<sub [::hp/temp])
      :save! #(>evt [::events/temp-hp! %2])}]]

   ; just a spacer
   [:div.new-hp]

   [:div.quick-adjust
    [:div.label "Extra Max HP"]

    [bind-fields
     [:input.number {:field :fast-numeric
                     :id :temp-max-hp
                     :min 0}]
     {:get #(<sub [::hp/temp-max])
      :save! #(>evt [::events/temp-max-hp! %2])}]]])


; ======= public interface ================================

(defn overlay []
  (r/with-let [state (r/atom {})]
    (let [[hp max-hp] (<sub [::hp/state])
          temp-hp (<sub [::hp/temp])
          {:keys [heal damage]} @state
          new-hp (max
                   0  ; you can't go negative in 5e
                   (min (+ max-hp temp-hp) ; don't collapse temp-hp above max
                        (- (+ hp heal)
                           damage)))]

      [:div styles/hp-overlay
       (when (= 0 hp)
         [saving-throws])

       [:h4 "Hit Points"]
       [hp-form
        :hp hp
        :max-hp max-hp]

       [:h5.centered.section-header "Quick Adjust"]
       [quick-adjust-form state
        :hp hp
        :max-hp max-hp
        :new-hp new-hp]

       ; temporary health management
       [:h5.centered.section-header "Temporary Health"]
       [temp-hp-form]

       ; this ought to get its own overlay at some point:
       [conditions-management]])))
