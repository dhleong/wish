(ns ^{:author "Daniel Leong"
      :doc "builder"}
  wish.sheets.dnd5e.builder
  (:require [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.util :refer [<sub >evt]]
            [wish.views.sheet-builder-util :refer [router]]))

(defn home-page []
  [:div
   [:h3 "Home"
    [:div
     [bind-fields
      [:div
       [:input {:field :text
                :id :name}]

       ; TODO pick data sources
       [:div
        [:p "Data Sources"
         "(TODO)"]]]

      {:get #(get-in (<sub [:sheet-meta]) %)
       :save! (fn [path v]
                (>evt [:update-meta path (constantly v)]))}]]]])

(defn feature-option
  [option]
  [:div.content
   [:b (:name option)]
   [:p (:desc option)]])

(defn feature-options-selection [sub-id]
  ; TODO React 16 supports returning multiple elements from a component....
  [:div.features
   (for [[feature-id f] (<sub [sub-id])]
     ^{:key feature-id}
     [:div.feature
      [:h3 (:name f)]

      [bind-fields
       ; FIXME TODO respect max-options
       [:div.feature-options {:field :single-select
                              :id feature-id}
        (for [option (:values f)]
          [:div.feature-option {:key (:id option)}
           ; NOTE: this extra widget with ^{:key} is a hack around how
           ; reagent-forms handles :single-select values. Basically,
           ; everything after the {:key} above seems to become a sequence,
           ; so react wants keys on all the children. It's a bit deep to
           ; put everything inline anyway, so we use this ^{:key} [component]
           ; pattern
           ^{:key :1}
           [feature-option option]])]

       {:get #(get (<sub [:options]) feature-id)
        :save! (fn [path v]
                 (>evt [:update-meta [:options]
                        assoc (first path)
                        (if (coll? v)
                          v
                          [v])]))}]])] )

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
   [feature-options-selection :race-features-with-options]

   ])

(defn class-section [class-info]
  [:div
   [:h2 (:name class-info)]
   [feature-options-selection :class-features-with-options] ])

(defn class-picker [unavailable-class-ids show-picker?]
  [:div
   [:h4 "Pick a new class"]
   [:div.feature-options
    (for [c (->> (<sub [:available-entities :classes])
                 (remove (comp unavailable-class-ids :id)))]
      ^{:key (:id c)}
      [:div.feature-option
       {:on-click (fn [e]
                    (.preventDefault e)
                    ; TODO first, show a preview of the class features

                    (println "Select " c)
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

(defn classes-page []
  (let [initial-classes (<sub [:classes])
        show-picker? (r/atom (empty? initial-classes))]
    (fn []
      (let [existing-classes (<sub [:classes])]
        [:div
         (for [c existing-classes]
           ^{:key (:id c)}
           [class-section c])

         (if @show-picker?
           [class-picker
            (->> existing-classes
                 (map :id)
                 (into #{}))
            show-picker?]

           [:div.pick-new-class
            [:a {:on-click (fn [e]
                             (.preventDefault e)
                             (swap! show-picker? not))}]])]))))

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
    [:div
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
         [:td (input-for :cha)]]]]
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

(def pages
  [[:home {:name "Home"
           :fn #'home-page}]
   [:race {:name "Race"
           :fn #'race-page}]
   [:class {:name "Class"
            :fn #'classes-page}]
   [:abilities {:name "Abilities"
                :fn #'abilities-page}]])

(defn view
  [section]
  [router pages (or section
                    :home)])
