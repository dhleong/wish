(ns ^{:author "Daniel Leong"
      :doc "builder"}
  wish.sheets.dnd5e.builder
  (:require-macros [wish.util.log :refer [log]])
  (:require [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [cljs-css-modules.macro :refer-macros [defstyle]]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.events :as events]
            [wish.util :refer [<sub >evt]]
            [wish.style.flex :as flex :refer [flex]]
            [wish.style.shared :as style]
            [wish.views.sheet-builder-util :refer [data-source-manager router
                                                   count-max-options]]
            [wish.views.widgets :refer [formatted-text]]
            [wish.views.widgets.limited-select]
            [wish.views.widgets.multi-limited-select]))

; ======= CSS ==============================================

(defstyle styles
  [:.abilities (merge flex/vertical
                      flex/align-center)
   [:table {:width "100%"
            :table-layout 'fixed}
    [:tbody {:text-align 'center}
     [:th {:font-size "9pt"
           :width "20%"}]
     [:td {:padding "4px"}
      [:input {:width "100%"
               :font-size "14pt"
               :text-align 'center}]]]]]

  [:.classes
   [:.meta style/metadata]

   [:.class-header (merge flex/vertical
                          {:margin "1.5em 0"})
    [:.name {:font-size "1.5em"
             :font-weight "bold"}]
    [:.row (merge flex
                  flex/align-center)
     [:select.level {:margin-left "12px"}]]]

   [:.hit-point-setting {:margin "8px"}
    [:.dice-level flex
     [:.level {:width "2em"
               :text-align 'center}]]]])


; ======= Pages ============================================


(defn home-page []
  [:div
   [:h3 "Home"
    [bind-fields
     [:div
      [:input {:field :text
               :id :name}] ]

     {:get #(get-in (<sub [:sheet-meta]) %)
      :save! (fn [path v]
               (>evt [:update-meta path (constantly v)]))}]

    ; data source mgmt
    [data-source-manager]]])

(defn feature-option
  [option]
  ; Fragment! avoids unnecessary extra parent
  [:<>
   [:b (:name option)]
   [formatted-text :div.desc (:desc option)]])

(defn expanding-assoc
  "Like (assoc), but safely expands vectors"
  [vect index v]
  (let [existing (count vect)]
    (if (and vect
             (or (<= index existing)))
      ; easy case
      (assoc vect index v)

      ; make space
      (let [before (- index existing)]
        (vec
          (concat
            vect
            (repeat before nil)
            [v]))))))

(defn expand-val
  "Given a feature, expand the value as necessary to handle
   instanced features, etc. Also ensures that the final value
   is a vector."
  [old-value feature path v]
  ; NOTE this code is pretty terrible and needs to be refactored..
  (let [{:keys [instanced?]} feature

        index (let [possibly-index (last path)]
                (when (number? possibly-index)
                  possibly-index))

        old-value (or old-value
                      (when instanced?
                        {:id (:id feature)}))

        v (cond
            (vector? v) v
            (coll? v) (vec v)
            :else [v])]
    (if instanced?
      (if index
        ; should be:
        (update old-value :value
                expanding-assoc index (first v))
        ; just in case
        (assoc-in old-value (drop 1 path) v))

      ; not instanced
      v)))

(defn limited-select-feature-options
  [f instance-id]
  [:div.feature-options {:field :limited-select
                         :accepted? (:max-options f)
                         :id instance-id}
   (for [option (:values f)]
     [:div.feature-option {:key (:id option)}
      ; NOTE: this extra widget with ^{:key} is a hack around how
      ; reagent-forms handles :single-select values. Basically,
      ; everything after the {:key} above seems to become a sequence,
      ; so react wants keys on all the children. It's a bit deep to
      ; put everything inline anyway, so we use this ^{:key} [component]
      ; pattern
      ^{:key (:id option)}
      [feature-option option]])])

(defn multi-select-feature-options
  [f instance-id]
  (let [max-options (count-max-options f)
        base-path (if (:wish/instance-id f)
                    [instance-id :value]
                    [instance-id])]
    (vec
      (cons
        :div.multi-feature-options
        (for [i (range max-options)]
          (vec
            (concat
              [:select {:field :multi-limited-select
                        :id (concat base-path [i])}
               [:option {:key :-none} "—Select One—"]]

              (for [o (:values f)]
                ^{:key (:id o)}
                [:option {:key (:id o)} (:name o)]))))))))

(defn- feature-options
  [f instance-id ]
  (if (:multi? f)
    (multi-select-feature-options f instance-id)
    (limited-select-feature-options f instance-id)))

(defn feature-options-selection [sub-vector]
  (if-let [features (seq (<sub sub-vector))]
    [:<>
     (for [[feature-id f] features]
       (let [instance-id (or (:wish/instance-id f)
                             feature-id)]
         ^{:key instance-id}
         [bind-fields
          [:div.feature
           [:h3
            (:name f)
            (when-let [n (:wish/instance f)]
              (str " #" (inc n)))]

           (feature-options f instance-id)]

          {:get #(<sub [:options-> %])
           :save! (fn [path v]
                    (>evt [:update-meta [:options]
                           update
                           (first path)
                           expand-val
                           f path v]))
           :doc #(<sub [:options])}])) ]

    ; no features
    [:p "No features with options available yet."]))

(defn race-page []
  [:div
   [:h3 "Race"]

   [bind-fields
    [:div.feature-options {:field :single-select
                           :id :races}
     (for [r (<sub [:available-entities :races])]
       [:div.feature-option {:key (:id r)}
        (:name r)])]

    {:get #(first (get-in (<sub [:sheet-meta]) [:races]))
     :save! (fn [_ v]
              (>evt [:update-meta [:races] (constantly [v])]))}]

   ; racial features
   [feature-options-selection [:race-features-with-options]]

   ])


; ======= class management/level-up ========================

(defn class-section [class-info]
  (log (<sub [:options]))
  [:div.class-section
   [:div.class-header
    [:div.row
     [:div.name (:name class-info)]

     [bind-fields
      [:select.level {:field :list
                      :id [(:id class-info) :level]}
       (for [level (range 1 21)]
         [:option
          {:key level}
          level])]

      {:get #(<sub [::subs/class-level (first %)])  ; % is [:class 'level]
       :save! (fn [path v]
                ; NOTE this seems to get triggered whenever this section is
                ; rendered for some reason...
                (>evt [:update-meta [:classes] assoc-in path v]))}]]

    (when (:primary? class-info)
      [:div.meta "Primary class"])]
   [feature-options-selection [::subs/class-features-with-options
                               (:id class-info)
                               (:primary? class-info)]]])

(defn class-picker [unavailable-class-ids show-picker?]
  [:div.class-picker
   [:h4 "Pick a new class"]
   [:div.feature-options
    (for [c (->> (<sub [:available-entities :classes])
                 (remove (comp unavailable-class-ids :id)))]
      ^{:key (:id c)}
      [:div.feature-option
       {:on-click (fn [e]
                    (.preventDefault e)
                    ; TODO first, show a preview of the class features

                    (>evt [:update-meta [:classes]
                           assoc (:id c)

                           ; build the class info map
                           (let [class-info {:id (:id c)
                                             :level 1}]
                             (if (empty? unavailable-class-ids)
                               ; first class
                               (assoc class-info :primary? true)
                               class-info))])
                    (reset! show-picker? false))}
       (:name c)])]])

(defn hit-point-manager
  [classes]
  (let [hit-die-by-class (reduce
                           (fn [m c]
                             (assoc m (:id c)
                                    (-> c :attrs :5e/hit-dice)))
                           {}
                           classes)]
    [:<>
     [:h2 "Hit Point Management"]
     [:p.meta
      "We don't yet support auto-average. Please input health rolled (or average) for each level below:"]

     [:div.sections
      (for [c classes]
        (let [die-size (get hit-die-by-class (:id c))]
          ^{:key (:id c)}
          [:div.hit-point-setting
           [:div.class (:name c)
            [:span.die (str " (D" die-size ")")]]

           (for [level (range (:level c))]
             ^{:key level}
             [bind-fields
              [:div.dice-level
               [:div.level (inc level)]
               [:div.hp [:input {:field :numeric
                                 :id [(:id c) level]
                                 :min 1
                                 :max die-size}]]]

              {:get #(<sub [::subs/rolled-hp %])
               :save! (fn [path v]
                        (let [v (min
                                  (get hit-die-by-class (first path))
                                  (max v 1))]
                          (>evt [::events/set-rolled-hp path v])))}])]))] ]))

(defn classes-page []
  (let [initial-classes (<sub [:classes])
        show-picker? (r/atom (empty? initial-classes))]
    (fn []
      (let [existing-classes (<sub [:classes])]

        [:div {:class (:classes styles)}
         [:h1 "Level Up"]

         ; hit points
         [hit-point-manager existing-classes]

         ; multiclassing
         (if @show-picker?
           [class-picker
            (->> existing-classes
                 (map :id)
                 (into #{}))
            show-picker?]

           [:div.pick-new-class
            [:h2 "Multiclassing"]
            [:a {:href "#"
                 :on-click (fn [e]
                             (.preventDefault e)
                             (swap! show-picker? not))}
             "Add another class"]])

         ; class feature config
         (for [c existing-classes]
           ^{:key (:id c)}
           [class-section c])]))))


; ======= ability scores ===================================

(defn- input-for
  [ability]
  [:input {:field :numeric
           :id ability
           :min 1
           :max 18}])

(defn abilities-page []
  (let [doc (r/atom {:str nil
                     :dex nil
                     :con nil
                     :int nil
                     :wis nil
                     :cha nil})]
    [:div {:class (:abilities styles)}
     [:h3 "Abilities"]
     [bind-fields
      [:table
       [:tbody
        [:tr
         [:th "STRENGTH"]
         [:th "DEXTERITY"]
         [:th "CONSTITUTION"]
         [:th "INTELLIGENCE"]
         [:th "WISDOM"]
         [:th "CHARISMA"]]

        [:tr
         [:td (input-for :str)]
         [:td (input-for :dex)]
         [:td (input-for :con)]
         [:td (input-for :int)]
         [:td (input-for :wis)]
         [:td (input-for :cha)]]

        ]]
      {:get (fn [path]
              (let [a (:abilities (<sub [:sheet]))]
                (get-in a path)))
       :save! (fn [path v]
                (>evt [:update-meta (concat [:sheet :abilities])
                       assoc-in
                       path
                       (min 18
                            (max 1
                                 (js/parseInt v)))]))}]
     ]))


; ======= backgrounds ======================================

(defn background-page []
  (let [primary-class (<sub [::subs/primary-class])
        chosen-background (<sub [:options-> [:background]])]
    [:div {:class (:background styles)}
     [:h1 "Background"]
     [feature-options-selection [::subs/background (:id primary-class)]]

     (when (= [:background/custom] chosen-background)
       [:<>
        [:h2 "Custom background"]
        [feature-options-selection
         [::subs/custom-background (:id primary-class)]]])
     ]))


; ======= router ===========================================

(def pages
  [[:home {:name "Home"
           :fn #'home-page}]
   [:race {:name "Race"
           :fn #'race-page}]
   [:abilities {:name "Abilities"
                :fn #'abilities-page}]
   [:background {:name "Background"
                 :fn #'background-page}]
   [:class {:name "Level Up"
            :fn #'classes-page}]])

(defn view
  [section]
  [router pages (or section
                    :home)])
