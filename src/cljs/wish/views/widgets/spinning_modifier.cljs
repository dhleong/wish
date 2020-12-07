(ns wish.views.widgets.spinning-modifier
  (:require [reagent.core :as r]
            [spade.core :refer [defclass]]
            [wish.views.widgets.circular-progress
             :refer [circular-progress]]))

(defclass spinning-modifier-class []
  {:display 'inline-block
   :position 'relative}

  [:.spinner {:touch-action 'none}]

  [:.value {:position 'absolute
            :left 0
            :right 0
            :text-align 'center
            :top :50%
            :transform "translateY(-50%)"
            }])

(defn compute-rotation [element last-touch this-touch]
  (let [[_ _ diameter] element  ; assumes a square
        circumference (* js/Math.PI diameter)
        [ox oy] last-touch
        [nx ny] this-touch
        ;; ox (- ox x) oy (- oy y)
        ;; nx (- nx x) ny (- ny y)
        dx (- ox nx)
        dy (- oy ny)
        distance (js/Math.sqrt (* dx dx)
                               (* dy dy))
        direction 1]
    (doto (* direction 360 (/ distance circumference))
      println)))

(defn on-touch-move [state-ref rotation-ref, ^js e]
  (let [touch (first (.-touches e))
        state @state-ref
        this-touch [(.-clientX touch) (.-clientY touch)]]
    (when-let [last-touch (:last-touch state)]
      (swap! rotation-ref + (compute-rotation
                              (:element state)
                              last-touch
                              this-touch)))
    (swap! state-ref assoc :last-touch this-touch)))

(defn spinning-modifier [ratom {:keys [path]}]
  (letfn [(<v []
            (get-in @ratom path 0))
          (>v [v]
            (assoc-in @ratom path v))]
    (r/with-let [initial (<v)
                 state (atom nil)
                 rotation (r/atom 0)]
      (let [current (<v)
            delta (- current initial)]
        [:div {:class (spinning-modifier-class)
               :on-touch-move (partial on-touch-move state rotation)}
         [:div.spinner {:ref #(when-let [el %]
                                (let [rect (.getBoundingClientRect el)]
                                  (swap! state assoc :element
                                         [(.-x rect) (.-y rect)
                                          (.-width rect)
                                          (.-height rect)])))
                        :style {:transform (str "rotate("
                                                @rotation
                                                "deg)")}}
          [circular-progress
           2 100 ;; TODO ?
           ;; delta 100
           :stroke-width 12
           :width 128]]

         [:div.value "42" delta current]
         ]))))
