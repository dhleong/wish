(ns wish.views.campaign.pages.notes
  (:require [spade.core :refer [defattrs]]
            [wish.style.flex :as flex]
            [wish.subs.campaign.notes :as notes]
            [wish.util :refer [<sub]]))


; ======= header ==========================================

(defattrs header-style []
  (flex/create
    :flow :vertical
    :center :vertical
    {:height "100%"
     :text-align 'right}))

(defn header []
  [:div (header-style)
   [:div.search "Search"]])

(defn- notes-list [notes]
  [:<>
   (for [n notes]
     [:div.note (str n)])])

(defn page []
  (let [notes (<sub [::notes/sorted])]
    (if-not (seq notes)
      [:div "No notes"]
      [notes-list notes])))
