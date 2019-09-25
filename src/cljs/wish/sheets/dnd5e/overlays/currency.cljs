(ns wish.sheets.dnd5e.overlays.currency
  (:require-macros [wish.util :refer [fn-click]]
                   [wish.util.log :as log :refer [log]])
  (:require [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.subs.inventory :as inventory]
            [wish.sheets.dnd5e.overlays.style :as styles]
            [wish.util :refer [<sub >evt]]
            [wish.views.widgets.fast-numeric]))

(def ^:private currencies [:platinum :gold :electrum :silver :copper])

(defn- generate-input-row
  ([] (generate-input-row nil))
  ([id-prefix]
   (into [:tr]
    (for [c currencies]
      (let [id (if id-prefix
                 (keyword (str (name id-prefix)
                               "."
                               (name c)))
                 c)]
        [:td
         [:input.amount {:field :fast-numeric
                         :id id}]])))))

(defn overlay []
  (r/with-let [quick-adjust (r/atom {})]
    [:div (styles/currency-manager-overlay)
     [:h5 "Currency"]
     [:form#currency-form
      {:on-submit (fn-click
                    (when-let [v (:adjust @quick-adjust)]
                      (log "Adjust currency: " v)
                      (>evt [::events/adjust-currency v]))
                    (>evt [:toggle-overlay nil]))}
      [bind-fields
       [:table
        [:tbody
         [:tr
          [:th.header.p [:span.label "Platinum"]]
          [:th.header.g [:span.label "Gold"]]
          [:th.header.e [:span.label "Electrum"]]
          [:th.header.s [:span.label "Silver"]]
          [:th.header.c [:span.label "Copper"]]]

         ; current values
         (generate-input-row)

         [:tr
          [:td.meta {:col-span 5}
           "Adjust totals directly"]]

         [:tr
          [:th {:col-span 5
                :style {:padding-top "1em"}}
           "Quick Adjust"]]

         (generate-input-row :adjust)

         [:tr
          [:td.meta {:col-span 5}
           "Add or subtract amounts by inputting positive or negative amounts above"]]

         ]]

       {:get #(if (= :adjust (first %))
                (get-in @quick-adjust %)
                (get-in (<sub [::inventory/currency]) %))

        :save! (fn [path v]
                 (if (not= :adjust (first path))
                   (>evt [::events/set-currency (first path) v])

                   (swap! quick-adjust assoc-in path v)))}]

      [:div.apply
       [:input.apply {:type 'submit
                      :value "Apply!"
                      :disabled (->> @quick-adjust
                                     :adjust
                                     vals
                                     (some number?)
                                     not)}]]]]))

