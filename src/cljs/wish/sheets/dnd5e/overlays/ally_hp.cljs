(ns wish.sheets.dnd5e.overlays.ally-hp
  (:require [reagent-forms.core :refer [bind-fields]]
            [spade.core :refer [defattrs]]
            [wish.sheets.dnd5e.overlays.style :as styles]
            [wish.util :refer [>evt <sub]]))

(defattrs overlay-attrs []
  styles/overlay
  [:.quick-edit {:display :flex
                 :flex-direction :row
                 :align-items :center
                 :justify-content :space-between}
   [:input {:font-size "1.2em"
            :width "5em"}]])

(defn overlay [ally]
  [:div (overlay-attrs)
   [:h3 "HP: " (:name ally)]
   [bind-fields
    [:div.quick-edit
     [:input.number {:field :fast-numeric
                     :id :hp
                     :min 0}]
     " / "
     [:input.number {:field :fast-numeric
                     :id :max-hp
                     :min 0}]]
    {:get (fn [path]
            (or (get-in (<sub [:ally-state ally]) path)
                (get-in ally path)))
     :save! #(>evt [:ally/set-in ally %1 %2])}]])
