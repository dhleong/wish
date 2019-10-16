(ns wish.views.campaign.pages.notes
  (:require [spade.core :refer [defattrs]]
            [wish.style.flex :as flex]
            [wish.style.media :as media]
            [wish.subs.campaign.notes :as notes]
            [wish.util :refer [<sub]]
            [wish.views.widgets :refer [icon link>evt]]))


; ======= new note overlay ================================

(defn new-note-overlay []
  [:div "New note"])

; ======= header ==========================================

(defattrs header-style []
  (flex/create
    :flow :vertical
    :center :vertical
    {:height "100%"
     :text-align 'right})
  (at-media media/smartphones
    {:text-align 'left})

  [:.new {:padding "4px 8px"}]
  [:.search {:padding "4px"}])

(defn header []
  [:div (header-style)
   [:div
    [link>evt {:class "new"
               :> [:toggle-overlay
                   [#'new-note-overlay]]}
     (icon :note-add)]]

   ; TODO
   #_[:div.search "Search"]])


; ======= list ============================================

(defn- notes-list [notes]
  [:<>
   (for [n notes]
     [:div.note (str n)])])

(defn page []
  (let [notes (<sub [::notes/sorted])]
    (if-not (seq notes)
      [:div "No notes"]
      [notes-list notes])))
