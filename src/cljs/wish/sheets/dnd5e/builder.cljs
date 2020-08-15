(ns ^{:author "Daniel Leong"
      :doc "builder"}
  wish.sheets.dnd5e.builder
  (:require-macros [wish.util :refer [fn-click]]
                   [wish.views.widgets :refer [icon]])
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [spade.core :refer [defattrs]]
            [wish.sheets.dnd5e.builder.data :as data]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.subs.abilities :as abilities]
            [wish.sheets.dnd5e.subs.builder :as builder]
            [wish.sheets.dnd5e.subs.hp :as hp]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.util :refer [mod->str]]
            [wish.style.media :as media]
            [wish.util :refer [<sub >evt click>reset! click>swap!]]
            [wish.style.flex :as flex :refer [flex]]
            [wish.style.shared :as style]
            [wish.views.sheet-builder-util :refer [availability-group
                                                   campaign-manager
                                                   data-source-manager
                                                   router
                                                   count-max-options
                                                   selected-option-counter]]
            [wish.views.widgets :refer [formatted-text]]
            [wish.views.widgets.dynamic-list]
            [wish.views.widgets.limited-select :refer [limited-select]]
            [wish.views.widgets.multi-limited-select]))

; ======= CSS ==============================================

(defattrs abilities-style []
  (merge flex/vertical
         flex/align-center)

  [:table {:width "100%"
           :table-layout 'fixed}
   [:tbody {:text-align 'center}
    [:th {:font-size "9pt"
          :width "20%"}]
    [:td {:padding "4px"}
     [:input {:width "100%"
              :font-size "14pt"
              :text-align 'center}]
     [:select {:width "100%"}]]]])

(defattrs classes-style []
  [:.meta style/metadata]

  [:.class-header (merge flex/vertical
                         {:margin "1.5em 0"})
   [:.name {:font-size "1.5em"
            :font-weight "bold"}]
   [:.row (merge flex
                 flex/align-center)
    [:select.level {:margin-left "12px"}]]]

  [:.group {:margin "8px 0"}]

  [:.hit-point-setting {:margin "8px"}
   [:.dice-level flex
    [:.level {:width "2em"
              :text-align 'center}]]]

  [:.hit-die
   [:.dice {:font-weight "bold"}]])

(defattrs feature-options-style []
  [:.feature
   [:.content {:padding "0 12px"}
    [:.desc style/metadata]
    [:.selected-count (merge style/metadata
                             {:font-weight 'bold})]]]

  ; features provided by another feature:
  [:.feature.provided {:margin-left "16px"}
   [:.title {:font-size "1.1em"}]]

  [:.title {:display 'flex
            :flex-direction 'row
            :align-items 'center}]

  [:.class.feature-option.disabled {:color "#ccc"
                                    :cursor 'default}
   [:.name {:font-style 'italic
            :text-decoration 'line-through}]
   [:.prereqs-reason {:color "#a00"}]])

(defattrs races-style []
  [:.subrace {:padding-left "1em"}])


; ======= Pages ============================================

(defn home-page []
  [:div
   [:h3 "Home"]
   [bind-fields
    [:div
     [:input {:field :text
              :id :name}] ]

    {:get #(get-in (<sub [:sheet-meta]) %)
     :save! (fn [path v]
              (>evt [:update-meta path (constantly v)]))}]

   ; campaign mgmt
   [campaign-manager]

   ; data source mgmt
   [data-source-manager]
   ])

(defn- feature-option
  [option selected?]
  ; Fragment! avoids unnecessary extra parent
  [:<>
   [:b (:name option)

    ; special case for spells:
    (when-let [spell-level (:spell-level option)]
      (if (= 0 spell-level)
        " · Cantrip"
        (str " · Level " spell-level)))]

   (when selected?
     [formatted-text :div.desc (:desc option)])])

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
        ; for :multi?
        (update old-value :value
                expanding-assoc index (first v))

        ; non-:multi?
        (assoc old-value :value v))

      ; not instanced
      v)))

(defn- limited-select-feature-option
  [{selected? :active? :as opts} option]
  [:div.feature-option (dissoc opts :active?)
   [feature-option option selected?]])

(defn- have-feature-option?
  [sub-vector feature-id instance-id option-id]
  (contains?
    (<sub [::builder/available-feature-options
           sub-vector
           feature-id
           instance-id])
    option-id))

(defn- limited-select-feature-options
  [f instance-id sub-vector extra-info doc]
  (let [available-options-set (<sub [::builder/available-feature-options
                                     sub-vector
                                     (:id f)
                                     instance-id])
        available-options (->> (:values f)
                               (filter #(contains? available-options-set (:id %))))
        max-options (let [max-options (:max-options f)]
                      (if (fn? max-options)
                        (max-options extra-info)
                        max-options))
        doc-path [instance-id]]
    [limited-select
     :accepted? max-options
     :doc doc
     :extra-info extra-info
     :options available-options
     :path doc-path
     :render-item (fn [opts option]
                    [limited-select-feature-option opts option])]))

(defn multi-select-feature-options
  [f instance-id sub-vector extra-info doc]
  (let [max-options (count-max-options f extra-info)
        base-path (if (:wish/instance-id f)
                    [instance-id :value]
                    [instance-id])]
    (into [:div.multi-feature-options]
          (for [i (range max-options)]
            [bind-fields
             (into
               [:select {:field :multi-limited-select
                         :id (concat base-path [i])}
                [:option {:key :-none} "—Select One—"]]

               (for [o (:values f)]
                 ^{:key (:id o)}
                 [:option {:key (:id o)
                           :visible? #(have-feature-option?
                                        sub-vector
                                        (:id f)
                                        instance-id
                                        (:id o))
                           }
                  (:name o)]))
             doc]))))

(defn- feature-options
  [f instance-id sub-vector extra-info doc]
  (if (:multi? f)
    [multi-select-feature-options
     f instance-id sub-vector
     extra-info doc]

    [limited-select-feature-options
     f instance-id sub-vector
     extra-info doc]))

(defn- single-feature-options-selection
  [sub-vector extra-info instance-id doc f]
  (let [max-options (count-max-options f extra-info)
        selected ((:get doc) [instance-id])
        selected-count (count selected)
        count-options? (not (:unrestricted-options? f))]
    [:div.feature {:class (when (> (count (:wish/sort f)) 2)
                           "provided")}
    [:h3.title
     (when count-options?
       [selected-option-counter
        selected-count
        max-options])

     (:name f)

     (when-let [n (:wish/instance f)]
       (str " #" (inc n)))]

    (when (and count-options?
               (< selected-count max-options))
      [:div.content>div.selected-count
       "Selected " selected-count " / " max-options])

    (when-let [group-ids (:availability-groups f)]
      [:div.content
       [availability-group instance-id group-ids]])

    [:div.content
     (when-let [desc (:desc f)]
       [formatted-text :div.desc desc])

     [feature-options f instance-id sub-vector extra-info doc]]]))

(defn feature-options-selection [sub-vector extra-info]
  (if-let [features (seq (<sub sub-vector))]
    [:div (feature-options-style)
     (for [[feature-id f] features]
       (let [instance-id (or (:wish/instance-id f)
                             feature-id)
             doc {:get #(<sub [:options-> %])
                  :save! (fn [path v]
                           (>evt [:update-meta [:options]
                                  update
                                  (first path)
                                  expand-val
                                  f path v]))
                  :doc #(<sub [:meta/options])}]

         ^{:key instance-id}
         [single-feature-options-selection
          sub-vector extra-info instance-id doc f])) ]

    ; no features
    [:p "No features with options available yet."]))

(defn race-page []
  [:div (races-style)
   [:h3 "Race"]

   [bind-fields
    [:div.feature-options {:field :single-select
                           :id :races}
     (for [r (<sub [::builder/available-races])]
       [:div.feature-option {:key (:id r)}
        ^{:key (:id r)}
        [:div {:class (when (:subrace-of r)
                        "subrace")}
         (:name r)]])]

    {:get #(first (<sub [:meta/races]))
     :save! (fn [_ v]
              (>evt [:update-meta [:races] (constantly [v])]))}]

   ; racial features
   [feature-options-selection [::builder/race-features-with-options]
    (<sub [:race])] ])


; ======= class management/level-up ========================

(defn class-section [class-info]
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
                (when-not (= v (<sub [::subs/class-level (first path)]))
                  (>evt [:update-class-level (first path) v])
                  #_(>evt [:update-meta [:classes] assoc-in path v])))}]

     [:div.remove.clickable
      {:title (str "Remove " (:name class-info) " Class")
       :on-click (fn-click
                   (when (js/confirm "Are you sure you want to remove this class?")
                     (>evt [::events/remove-class class-info])))}
      (icon :clear)]]

    (when (:primary? class-info)
      [:div.meta "Primary class"])]
   [feature-options-selection [::builder/class-features-with-options
                               (:id class-info)]
    class-info]

   ])

(defn class-picker [unavailable-class-ids show-picker?]
  [:div.class-picker (feature-options-style)
   [:h4 "Pick a new class\u00A0"
    (when-not (empty? unavailable-class-ids)
      [:a {:href "#"
           :on-click (click>reset! show-picker? false)}
       "Cancel"])]

   [:div.feature-options
    (for [c (<sub [::builder/available-classes])]
      ^{:key (:id c)}
      [:div.class.feature-option
       {:class (when (:prereqs-failed? c)
                 "disabled")
        :on-click (when-not (:prereqs-failed? c)
                    (fn-click
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
                      (reset! show-picker? false)))}

       [:div.name (:name c)]
       (when (:prereqs-failed? c)
         [:div.prereqs-reason (:prereqs-reason c)])])]])

(defn- hp-mode-average []
  [:<>
   [:p.meta
    "Your Max HP is determined by the average roll on your class's hit die
     for each level in that class. You get the maximum roll for the first
     level in your primary class."]
   [:div
    [:b "Total Max HP: "]
    (<sub [::hp/max])]

   [:div.group
    [:b "Hit Dice: "]
    [:div (for [{:keys [die classes total]} (<sub [::hp/hit-dice])]
            ^{:key die}
            [:div.hit-die
             [:span.dice total "d" die]
             [:span.classes " (" (str/join ", " classes) ")"]])]]])

(defn- hp-mode-manual []
  (let [hit-die-by-class (<sub [::builder/class->hit-die])]
    [:<>
     [:p.meta
      "Input health rolled (or average) for each level below:"]

     [:div.sections
      (for [[id {die-size :dice :as c}] hit-die-by-class]
        ^{:key id}
        [:div.hit-point-setting
         [:div.class (:name c)
          [:span.die (str " (D" die-size ")")]]

         (for [level (range (:level c))]
           ^{:key level}
           [bind-fields
            [:div.dice-level
             [:div.level (inc level)]
             [:div.hp [:input {:field :numeric
                               :id [id level]
                               :min 1
                               :max die-size}]]]

            {:get #(<sub [::hp/rolled %])
             :save! (fn [path v]
                      (let [v (min
                                (:dice (get hit-die-by-class (first path)))
                                (max v 1))]
                        (>evt [::events/set-rolled-hp path v])))}])])]]))

(defn hit-point-manager []
  (when-not (empty? (<sub [:classes]))
    [:<>
     [:h2
      "Hit Point Management "
      [bind-fields
       [:select {:id :max-hp-mode
                 :field :list}
        [:option {:key :average} "Automatic"]
        [:option {:key :manual} "Manual"]]
       {:get #(<sub [::hp/max-hp-mode])
        :save! #(when-not (= %2 (<sub [::hp/max-hp-mode]))
                  (>evt [:update-meta [:sheet]
                         assoc
                         :max-hp-mode %2]))}]]

     (case (<sub [::hp/max-hp-mode])
       :average [hp-mode-average]
       :manual [hp-mode-manual])

     ]))

(defn classes-page []
  (r/with-let [initial-classes (<sub [:classes])
               show-picker? (r/atom (empty? initial-classes))]
    (let [existing-classes (<sub [:classes])]

      [:div (classes-style)
       [:h1 "Level Up"]

       ; hit points
       [hit-point-manager]

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
               :on-click (click>swap! show-picker? not)}
           "Add another class"]])

       ; class feature config
       (for [c existing-classes]
         ^{:key (:id c)}
         [class-section c])])))


; ======= ability scores ===================================

(def ^:private standard-array-scores [8 10 12 13 14 15])

(defn- input-for
  [mode ability]
  (case mode
    :manual [:input {:field :numeric
                     :id ability
                     :min 1
                     :max 18}]

    :standard (into [:select {:field :list
                              :id ability}
                     [:option {:key :-none} "—"]]

                    (for [score standard-array-scores]
                      [:option {:key score
                                :visible? (fn [doc]
                                            (or (= (get doc ability)
                                                   score)
                                                (not (contains?
                                                       (->> doc
                                                            vals
                                                            set)
                                                       score))))}
                       (str score)]))

    :point [:select.point-buy {:field :dynamic-list
                               :id ability}
            (for [[score cost] data/score-point-cost]
              [:option {:key score
                        :visible? (fn [_doc]
                                    (let [available (<sub [::builder/point-buy-remaining])
                                          delta (<sub [::builder/point-buy-delta ability cost])]
                                      (>= (+ available delta)
                                          0)))
                        :content (fn [_doc]
                                   (let [delta (<sub [::builder/point-buy-delta ability cost])
                                         delta-mod (if (> delta 0)
                                                     "+"
                                                     "-")
                                         delta-suffix (when-not (= delta 0)
                                                        (str " ("
                                                             delta-mod
                                                             (Math/abs delta)
                                                             " Points)"))]
                                     (str score delta-suffix)))}
               score])]))

(defn- abilities-form [mode]
  [bind-fields

   [:tr
    [:td (input-for mode :str)]
    [:td (input-for mode :dex)]
    [:td (input-for mode :con)]
    [:td (input-for mode :int)]
    [:td (input-for mode :wis)]
    [:td (input-for mode :cha)]]

   {:get (fn [path]
           (let [a (:abilities (<sub [:meta/sheet]))]
             (get-in a path)))
    :doc #(:abilities (<sub [:meta/sheet]))
    :save! (fn [path v]
             (if (= :-none v)
               ; unset
               (>evt [:update-meta [:sheet :abilities]
                      dissoc
                      (first path)])

               ; parse and set
               (>evt [:update-meta [:sheet :abilities]
                      assoc-in
                      path
                      (min 18
                           (max 1
                                (js/parseInt v)))])))}])

;; NOTE: because bind-forms doesn't play nicely with changing the underlying
;; form fields, we create a separate version of the form component for each type.
;; Yuck.
(def ^:private manual-form (partial abilities-form :manual))
(def ^:private standard-form (partial abilities-form :standard))
(def ^:private point-form (partial abilities-form :point))

(def labeled-abilities
  [[:str "STRENGTH"]
   [:dex "DEXTERITY"]
   [:con "CONSTITUTION"]
   [:int "INTELLIGENCE"]
   [:wis "WISDOM"]
   [:cha "CHARISMA"]])

(defn- bonuses-from [label sub-vector]
  (let [bonuses (<sub sub-vector)]
    [:<>
     [:tr
      [:th {:col-span 6}
       label]]

     [:tr
      (for [[id _] labeled-abilities]
        ^{:key id}
        [:td (if-let [b (get bonuses id)]
               (mod->str b)
               "—")])]]))

(defattrs ability-label [label]
  [:&:after {:content (str "'" label "'")}]
  (at-media media/smartphones
    [:&:after {:content (str "'" (subs label 0 3) "'")}]))

(defn abilities-page []
  (let [mode (<sub [::builder/abilities-mode])]
    [:div (abilities-style)
     [:h3 "Abilities"]

     [:div
      [:h4
       "Input mode: "
       [bind-fields
        [:<>
         [:select {:field :list
                   :id :abilities-mode}
          [:option {:key :manual} "Manual"]
          [:option {:key :standard} "Standard Array"]
          [:option {:key :point} "Point Buy"]]]
        {:get #(<sub [::builder/abilities-mode])
         :save! #(>evt [:update-meta [:sheet]
                        assoc
                        :abilities-mode %2])}]]]

     (when (= :point mode)
       [:<>
        [:h5
         "Remaining Points: "
         (<sub [::builder/point-buy-remaining])]])

     [:table
      [:tbody
       [:tr
        (for [[id label] labeled-abilities]
          ^{:key id}
          [:th (ability-label label)])]

       ; see comment on the definition of these vars above
       (case mode
         :manual [manual-form]
         :standard [standard-form]
         :point [point-form])

       [bonuses-from "Racial Bonuses" [::abilities/racial]]
       [bonuses-from "Ability Score Improvements" [::abilities/improvements]]

       (let [abilities (<sub [::abilities/base])]
         [:<>
          [:tr
           [:th {:col-span 6}
            "Total Scores"]]
          [:tr
           (for [[id _] labeled-abilities]
             ^{:key id}
             [:td (get abilities id)])]])
       ]]
     ]))


; ======= backgrounds ======================================

(defn background-page []
  (let [primary-class (<sub [::builder/primary-class])
        chosen-background (<sub [:options-> [:background]])]
    [:div
     [:h1 "Background"]
     [feature-options-selection [::builder/background (:id primary-class)] nil]

     (when (= [:background/custom] chosen-background)
       [:<>
        [:h2 "Custom background"]
        [feature-options-selection
         [::builder/custom-background (:id primary-class) nil]]])
     ]))


; ======= router ===========================================

(defattrs icon-nav-attrs []
  {:padding "0 8px"})

(defn icon-nav [ico]
  [:div (icon-nav-attrs)
   ico])

(def pages
  [[:home {:name "Config"
           :icon [icon-nav (icon :app-settings-alt)]
           :fn #'home-page}]
   [:race {:name "Race"
           :icon [icon-nav (icon :emoji-people)]
           :fn #'race-page}]
   [:abilities {:name "Abilities"
                :icon [icon-nav (icon :school)]
                :fn #'abilities-page}]
   [:background {:name "Background"
                 :icon [icon-nav (icon :history-edu)]
                 :fn #'background-page}]
   [:class {:name "Level Up"
            :icon [icon-nav (icon :plus-one)]
            :fn #'classes-page}]])

(defn view
  [section]
  [router pages (or section
                    :home)])
