(ns ^{:author "Daniel Leong"
      :doc "builder"}
  wish.sheets.dnd5e.builder
  (:require [reagent-forms.core :refer [bind-fields]]
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

(def pages
  [[:home {:name "Home"
           :fn #'home-page}]
   [:race {:name "Race"
           :fn #'race-page}]])

(defn view
  [section]
  [router pages (or section
                    :home)])
