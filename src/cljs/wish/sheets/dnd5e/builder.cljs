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

      {:get #(get-in (<sub [:sheet-meta]) %)
       :save! (fn [path v]
                (>evt [:update-meta path (constantly v)]))}]]]])

(defn race-page []
  [:div
   [:h3 "Race"]])

(def pages
  [[:home {:name "Home"
           :fn #'home-page}]
   [:race {:name "Race"
           :fn #'race-page}]])

(defn view
  [section]
  [router pages (or section
                    :home)])
