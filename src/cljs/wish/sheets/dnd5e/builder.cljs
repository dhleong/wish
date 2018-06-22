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
      [:input {:field :text
               :id :name}]

      ; TODO pick data sources
      [:div
       [:p "Data Sources"
        "(TODO)"]]

      {:get #(get-in (<sub [:sheet-meta]) %)
       :save! (fn [path v]
                (>evt [:update-meta path (constantly v)]))}]]]])

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
              (>evt [:update-meta [:races] (constantly [v])]))}]])


(defn class-section [class-info]
  [:div
   [:h4 (:name class-info)]])

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
         [:h3 "Class"]
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

(def pages
  [[:home {:name "Home"
           :fn #'home-page}]
   [:race {:name "Race"
           :fn #'race-page}]
   [:class {:name "Class"
            :fn #'classes-page}]])

(defn view
  [section]
  [router pages (or section
                    :home)])
