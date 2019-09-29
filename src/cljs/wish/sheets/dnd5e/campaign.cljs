(ns ^{:author "Daniel Leong"
      :doc "Campaign-viewer for D&D 5e"}
  wish.sheets.dnd5e.campaign
  (:require [wish.sheets.dnd5e.subs.abilities :as abilities]
            [wish.sheets.dnd5e.subs.hp :as hp]
            [wish.sheets.dnd5e.views.abilities :refer [abilities-display]]
            [wish.sheets.dnd5e.views.header :refer [hp-death-saving-throws]]
            [wish.sheets.dnd5e.campaign.style :as style]
            [wish.views.campaign.base :as base]
            [wish.views.campaign.hp-bar :refer [hp-bar]]
            [wish.util :refer [<sub]]))

(defn char-card [{:keys [id] :as c}]
  [:div (style/char-card)
   [:div.name (:name c)]

   [:div.hp
    (let [[hp max-hp] (<sub [::hp/state id])]
      (if (> hp 0)
        [hp-bar hp max-hp]
        [hp-death-saving-throws id]))]

   [:div.abilities
    (let [info (<sub [::abilities/info id])]
      [abilities-display info])]
   ])

(defn entity-card [e]
  ; TODO
  [:div.entity-card (str e)])

(defn view
  [section]
  [base/campaign-page section
   :char-card char-card
   :entity-card entity-card])
