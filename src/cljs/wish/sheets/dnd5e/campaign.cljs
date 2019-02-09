(ns ^{:author "Daniel Leong"
      :doc "Campaign-viewer for D&D 5e"}
  wish.sheets.dnd5e.campaign
  (:require [wish.sheets.dnd5e.subs :as subs]
            [wish.sheets.dnd5e :as dnd5e]
            [wish.sheets.dnd5e.campaign.style :as style]
            [wish.views.campaign.base :as base]
            [wish.views.campaign.hp-bar :refer [hp-bar]]
            [wish.views.widgets :as widgets
             :refer-macros [icon]
             :refer [link link>evt]]
            [wish.util :refer [<sub >evt]]))

(defn char-card [{:keys [id] :as c}]
  [:div style/char-card
   [:div.name (:name c)]

   [:div.hp
    (let [[hp max-hp] (<sub [::subs/hp id])]
      (if (> hp 0)
        [hp-bar hp max-hp]
        [dnd5e/hp-death-saving-throws id]))]

   [:div.abilities
    (let [info (<sub [::subs/ability-info id])]
      [dnd5e/abilities-display info])]
   ])

(defn view
  [section]
  [base/campaign-page section
   :char-card char-card])
