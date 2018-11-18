(ns ^{:author "Daniel Leong"
      :doc "hp-bar"}
  wish.views.campaign.hp-bar)

(defn hp-bar [hp max-hp]
  [:div.hp-bar
   [:div.bar {:style {:width (str (int
                                    (* 100
                                       (/ hp max-hp)))
                                  "%")}}]
   [:div.label
    hp " / " max-hp]
   ])
