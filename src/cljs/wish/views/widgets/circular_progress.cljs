(ns wish.views.widgets.circular-progress
  (:require [garden.color :as color]
            [garden.units :refer [px]]
            [spade.core :refer [defattrs]]
            [wish.style :as theme]
            [wish.style.media :as media]))

(defattrs circular-progress-attrs [width circumference stroke-width]
  {:height (px width)
   :width (px width)}

  [:.slot {:stroke-width stroke-width
           :stroke (color/transparentize theme/text-primary-on-light 0.75)}
   (at-media media/dark-scheme
     {:stroke (color/transparentize theme/text-primary-on-dark 0.75)})]

  [:.circle {:stroke-dasharray [[circumference circumference]]
             :stroke-width stroke-width
             :stroke theme/text-primary-on-light

             :transition [[:stroke-dashoffset "0.35s"]]
             :transform "rotate(-90deg)"
             :transform-origin [[:50% :50%]]}
   (at-media media/dark-scheme
     {:stroke theme/text-primary-on-dark})])

(defn circular-progress
  [current max & {:keys [stroke-width width]
                  :or {stroke-width 4
                       width 32}}]
  (let [radius (* 0.5 width)
        inner-radius (- (/ width 2) (* 2 stroke-width))
        circumference (* 2 inner-radius js/Math.PI)
        perc (/ current max)]
    [:svg (circular-progress-attrs width circumference stroke-width)
     [:circle.slot {:fill 'transparent
                    :cx radius
                    :cy radius
                    :r inner-radius}]
     [:circle.circle {:fill 'transparent
                      :cx radius
                      :cy radius
                      :r inner-radius

                      :stroke-dashoffset (* circumference (- 1.0 perc))}]]))

