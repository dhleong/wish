(ns wish.views.widgets.spinning-modifier
  (:require [reagent.core :as r]
            [spade.core :refer [defattrs]]
            [wish.views.widgets.circular-progress
             :refer [circular-progress]]))

(defattrs spinning-modifier-attrs []
  {:display 'inline-block
   :position 'relative}

  [:.value {:position 'absolute
            :left 0
            :right 0
            :text-align 'center
            :top :50%
            :transform "translateY(-50%)"
            }])

(defn spinning-modifier [ratom {:keys [path]}]
  (letfn [(<v []
            (get-in @ratom path 0))
          (>v [v]
            (assoc-in @ratom path v))]
    (r/with-let [initial (<v)]
      (let [current (<v)
            delta (- current initial)]
        [:div (spinning-modifier-attrs)
         [circular-progress
          delta 100
          :stroke-width 12
          :width 128]

         [:div.value "42" current]
         ]))))
