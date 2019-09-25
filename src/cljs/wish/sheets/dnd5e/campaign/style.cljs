(ns ^{:author "Daniel Leong"
      :doc "campaign.style"}
  wish.sheets.dnd5e.campaign.style
  (:require [spade.core :refer [defattrs]]
            [wish.style.flex :as flex :refer [flex]]))

(defattrs char-card []
  [:.abilities (merge flex
                      flex/justify-center)
   [:.label {:font-size "0.7em"}]
   [:.mod {:font-size "1.5em"
           :padding "2px 4px"}]
   [:.score {:font-size "0.9em"
             :margin-bottom "8px"}]
   ])
