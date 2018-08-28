(ns ^{:author "Daniel Leong"
      :doc "builder"}
  wish.sheets.dnd5e.builder
  (:require-macros [wish.util :refer [fn-click]]
                   [wish.util.log :refer [log]]
                   [wish.views.widgets :refer [icon]])
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.events :as events]
            [wish.util :refer [<sub >evt click>reset! click>swap!]]
            [wish.style :refer-macros [defclass defstyled]]
            [wish.style.flex :as flex :refer [flex]]
            [wish.style.shared :as style]
            [wish.views.sheet-builder-util :refer [data-source-manager router
                                                   count-max-options]]
            [wish.views.widgets :refer [formatted-text]]
            [wish.views.widgets.limited-select]
            [wish.views.widgets.multi-limited-select]))

; ======= CSS ==============================================

(defstyled abilities-style
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
              :text-align 'center}]]]])

(defstyled classes-style
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
              :text-align 'center}]]])

(defstyled feature-options-style
  [:.feature>.content {:padding "0 12px"}
   [:.desc style/metadata]])

(defstyled races-style
  [:.subrace {:padding-left "1em"}])


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
  ([option]
   (feature-option option :selected))
  ([option selected?]
   ; Fragment! avoids unnecessary extra parent
   [:<>
    [:b (:name option)

     ; special case for spells:
     (when-let [spell-level (:spell-level option)]
       (if (= 0 spell-level)
         " · Cantrip"
         (str " · Level " spell-level)))]

    (when selected?
      [formatted-text :div.desc (:desc option)])]))

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
  [{selected? :active? :as opts} [option]]
  [:div.feature-option (dissoc opts :active?)
   [feature-option option selected?]])

(defn limited-select-feature-options
  [f instance-id sub-vector extra-info]
  (let [total-items (count (:values f))
        scrollable? (>= total-items 15)]
    [:div.feature-options {:class (when scrollable?
                                    "scrollable")
                           :field :limited-select
                           :accepted? (:max-options f)
                           :accepted?-extra extra-info
                           :id instance-id}
     (for [option (:values f)]
       ; being able to use a fn here is only because :limited-select is a
       ; custom widget, but it's nice because the map we pass below gets
       ; updated to include :active?, with which we can limit how much
       ; we render—very helpful for perf on large lists. One tricky bit
       ; about this usage, though, is that the body is passed through as
       ; a sequence, so we need to destructure it (see above)
       [limited-select-feature-option
        {:key (:id option)
         ; NOTE: reagent-forms doesn't properly handle dynamically
         ; changing option lists for a feature
         :visible?! #(<sub [::subs/have-feature-option?
                            sub-vector
                            (:id f)
                            (:id option)])
         }
        option])]))

(defn multi-select-feature-options
  [f instance-id sub-vector extra-info]
  (let [max-options (count-max-options f extra-info)
        base-path (if (:wish/instance-id f)
                    [instance-id :value]
                    [instance-id])]
    (into [:div.multi-feature-options]
          (for [i (range max-options)]
            (into
              [:select {:field :multi-limited-select
                        :id (concat base-path [i])}
               [:option {:key :-none} "—Select One—"]]

              (for [o (:values f)]
                ^{:key (:id o)}
                [:option {:key (:id o)
                          :visible? #(<sub [::subs/have-feature-option?
                                            sub-vector
                                            (:id f)
                                            (:id o)])}
                 (:name o)]))))))

(defn- feature-options
  [f instance-id sub-vector extra-info-atom]
  (if (:multi? f)
    (multi-select-feature-options f instance-id sub-vector @extra-info-atom)
    (limited-select-feature-options f instance-id sub-vector extra-info-atom)))

(defn feature-options-selection [sub-vector source-info]
  ; NOTE: thanks to how reagent-forms completely disregards changed
  ; inputs on subsequent renders, we have to store changes extra-info
  ; in an atom and dereference it in limited-select-feature-options
  ; extra-info, provided by callers, is mostly interesting for :level
  ; since many things scale by level
  (let [extra-info-atom (atom nil)]
    (fn [sub-vector source-info]
      (if-let [features (seq (<sub sub-vector))]
        [:div feature-options-style
         (for [[feature-id f :as entry] features]
           (let [instance-id (or (:wish/instance-id f)
                                 feature-id)
                 extra-info (dissoc source-info :features :limited-uses :&levels)]
             ; see NOTE above
             (reset! extra-info-atom extra-info)

             ^{:key instance-id}
             [bind-fields
              [:div.feature
               [:h3
                (:name f)
                (when-let [n (:wish/instance f)]
                  (str " #" (inc n)))]

               [:div.content
                (when-let [desc (:desc f)]
                  [formatted-text :div.desc desc])

                (feature-options f instance-id sub-vector extra-info-atom)]]

              {:get #(<sub [:options-> %])
               :save! (fn [path v]
                        (>evt [:update-meta [:options]
                               update
                               (first path)
                               expand-val
                               f path v]))
               :doc #(<sub [:meta/options])}])) ]

        ; no features
        [:p "No features with options available yet."]))))

(defn race-page []
  [:div races-style
   [:h3 "Race"]

   [bind-fields
    [:div.feature-options {:field :single-select
                           :id :races}
     (for [r (<sub [::subs/available-races])]
       [:div.feature-option {:key (:id r) }
        [:div {:class (when (:subrace-of r)
                        "subrace")}
         (:name r)]])]

    {:get #(first (<sub [:meta/races]))
     :save! (fn [_ v]
              (>evt [:update-meta [:races] (constantly [v])]))}]

   ; racial features
   [feature-options-selection [::subs/race-features-with-options]
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
                (>evt [:update-meta [:classes] assoc-in path v]))}]

     [:div.remove.clickable
      {:title (str "Remove " (:name class-info) " Class")
       :on-click (fn-click
                   (when (js/confirm "Are you sure you want to remove this class?")
                     (>evt [::events/remove-class class-info])))}
      (icon :clear)]]

    (when (:primary? class-info)
      [:div.meta "Primary class"])]
   [feature-options-selection [::subs/class-features-with-options
                               (:id class-info)
                               (:primary? class-info)]
    class-info]

   ])

(defn class-picker [unavailable-class-ids show-picker?]
  [:div.class-picker
   [:h4 "Pick a new class\u00A0"
    [:a {:href "#"
         :on-click (click>reset! show-picker? false)}
     "Cancel"]]

   [:div.feature-options
    (for [c (<sub [::subs/available-classes])]
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

        [:div classes-style
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
                 :on-click (click>swap! show-picker? not)}
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
    [:div abilities-style
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
              (let [a (:abilities (<sub [:meta/sheet]))]
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
    [:div
     [:h1 "Background"]
     [feature-options-selection [::subs/background (:id primary-class)] nil]

     (when (= [:background/custom] chosen-background)
       [:<>
        [:h2 "Custom background"]
        [feature-options-selection
         [::subs/custom-background (:id primary-class) nil]]])
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
