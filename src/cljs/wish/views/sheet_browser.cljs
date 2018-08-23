(ns ^{:author "Daniel Leong"
      :doc "sheet-browser"}
  wish.views.sheet-browser
  (:require [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.util :refer [<sub]]
            [wish.util.nav :refer [sheet-url]]
            [wish.views.widgets :refer [link]]))

(defn- sub-from [settings]
  (cond
    (and (:mine? settings)
         (:shared? settings))
    [:known-sheets]

    (:mine? settings)
    [:my-known-sheets]

    (:shared? settings)
    [:shared-known-sheets]))

(defn page []
  (r/with-let [settings (r/atom {:mine? true})]

    [:div
     [:h3 "Sheets"]

     [bind-fields
      [:div

       [:input {:field :checkbox
                :id :mine?}]
       [:label {:for :mine?}
        "My Sheets"]

       [:input {:field :checkbox
                :id :shared?}]
       [:label {:for :shared?}
        "Shared Sheets"]]

      settings
      (fn [id value {:keys [mine? shared?] :as doc}]
        (when-not (or mine? shared?)
          ; if neither is checked, auto-check the "other"
          ; one after unchecking the last
          (case (first id)
            :mine? (assoc doc :shared? true)
            :shared? (assoc doc :mine? true))))]

     (when (<sub [:providers-listing?])
       [:div.loading "Loading..."])

     [:ul
      (for [s (<sub (sub-from @settings))]
        ^{:key (:id s)}
        [:li.sheet-link
         [link {:href (sheet-url (:id s))}
          (:name s)]])]

     [:div
      [link {:href "/sheets/new"}
       "Create a new sheet"]]]))
