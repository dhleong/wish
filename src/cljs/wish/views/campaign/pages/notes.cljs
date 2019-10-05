(ns wish.views.campaign.pages.notes
  (:require [wish.subs.campaign.notes :as notes]
            [wish.util :refer [<sub]]))

(defn- notes-list [notes]
  [:<>
   (for [n notes]
     [:div.note (str n)])])

(defn page []
  (let [notes (<sub [::notes/sorted])]
    (if-not (seq notes)
      [:div "No notes"]
      [notes-list notes])))
