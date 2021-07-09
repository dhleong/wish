(ns wish.sheets.dnd5e.views.shared
  (:require [spade.core :refer [defattrs]]
            [wish.style.flex :as flex]
            [wish.util :refer [<sub]]
            [wish.views.widgets.error-boundary :refer [error-boundary]]
            [wish.sheets.dnd5e.subs :as subs]))

(defn buff-kind->attrs [buff-kind]
  (when buff-kind
    {:class (str (name buff-kind) "ed")}))

(defn buff-value->kind [buffs]
  (cond
    (> buffs 0) :buff
    (< buffs 0) :nerf))

(defn buff-kind-attrs-from-path [& path]
  (->> (<sub (into [::subs/buffs] path))
       buff-value->kind
       buff-kind->attrs))

(defattrs challenge-indicator-attrs [inline?]
  (merge (if inline?
           flex/flex
           flex/vertical)
         flex/center
         {:padding-right "8px"
          :display (when inline?
                     :inline-flex)})
  [:.label {:font-size "80%"}])

(defn challenge-indicator
  ([rating] (challenge-indicator nil rating))
  ([{:keys [inline?]} rating]
   [:div (challenge-indicator-attrs inline?)
    [:div.label "CR" (when inline?
                       "\u00A0")]
    [:div.value (case rating
                  0.125 "⅛"
                  0.25 "¼"
                  0.5 "½"
                  rating)]]))


(defn section
  ([title content]
   (section title nil content))
  ([title section-style content]
   (let [opts (or section-style
                  {})]
     [:div.section opts
      [:h1 title]
      [error-boundary
       content]])))

