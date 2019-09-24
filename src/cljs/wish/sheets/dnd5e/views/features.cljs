(ns wish.sheets.dnd5e.views.features
  (:require [wish.util :refer [<sub click>evt]]
            [wish.sheets.dnd5e.overlays :as overlays]
            [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e.views.actions :as actions]
            [wish.views.widgets :refer [formatted-text]]))


; ======= Features =========================================

(defn feature [f]
  (let [values (seq (:values f))]
    [:div.feature
     [:div.name (:name f)]

     [actions/consume-use-block f {:omit-name (:name f)}]

     [formatted-text :div.desc (:desc f)]

     (when values
       [:div.chosen-details
        [:h5 "Chosen values:"]
        (for [v values]
          ^{:key (:id v)}
          [:div.chosen.clickable
           {:on-click (click>evt [:toggle-overlay
                                  [#'overlays/info v]])}
           (:name v)])])]))

(defn view []
  [:<>
   (when-let [fs (<sub [::subs/features-for [:inflated-class-features]])]
      [:div.features-category
       [:h3 "Class features"]
       (for [f fs]
         ^{:key (:id f)}
         [feature f])])

    (when-let [fs (<sub [::subs/features-for [:inflated-race-features]])]
      [:div.features-category
       [:h3 "Racial Traits"]
       (for [f fs]
         ^{:key (:id f)}
         [feature f])])

    ; TODO proficiencies?
    ; TODO feats?
    ])



