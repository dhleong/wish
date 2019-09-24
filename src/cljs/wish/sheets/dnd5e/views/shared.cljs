(ns wish.sheets.dnd5e.views.shared
  (:require [wish.util :refer [<sub]]
            [wish.views.error-boundary :refer [error-boundary]]
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

