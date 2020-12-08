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
            :transform "translateY(-50%)"}])

(defn- polar-angle
  "Given a box circumscribing a circle and a point relative to that box,
   compute the polar coordinate angle of that point.  In other words,
   project the point onto the circumference of the circle, and get the
   angle of the equivalent polar coordinate.

   0 degrees is 'east'; 90 is 'south', etc"
  [element point]
  (let [[x y w] element
        [px py] point

        ; shift the point so the box has origin at 0 0
        ; with radius w/2
        radius (/ w 2)
        px (- px x radius)
        py (- py y radius)]

    ; from: https://math.stackexchange.com/a/1744369
    (js/Math.atan2 py px)))

(def ^:private two-pi (* js/Math.PI 2))

(defn compute-rotation [element last-touch this-touch]
  (let [last-angle (polar-angle element last-touch)
        this-angle (polar-angle element this-touch)
        delta (- this-angle last-angle)
        normalized (cond
                     (> delta js/Math.PI) (- two-pi delta)
                     (< delta (- js/Math.PI)) (+ two-pi delta)
                     :else delta)]
    (-> normalized

        ; convert to degrees:
        (* 180)
        (/ js/Math.PI))))

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

(defn spinning-modifier [ratom & {:keys [initial maximum path]}]
  (letfn [(<v []
            (get-in @ratom path 0))
          (>v [v]
            (assoc-in @ratom path v))]
    (r/with-let [state (atom nil)
                 rotation (r/atom 0)]
      (let [delta (int (* maximum (/ @rotation 360)))
            current (+ initial delta)]
        [:div {:class (spinning-modifier-class)
               :on-touch-move (partial on-touch-move state rotation)
               :on-touch-end #(swap! state dissoc :last-touch)}
         [:div.spinner {:ref #(when-let [el %]
                                (let [rect (.getBoundingClientRect el)]
                                  (swap! state assoc :element
                                         [(.-x rect) (.-y rect)
                                          (.-width rect)
                                          (.-height rect)])))
                        #_:style #_{:transform (str "rotate("
                                                @rotation
                                                "deg)")}}
          [circular-progress
           delta maximum
           ;; delta 100
           :stroke-width 12
           :width 128]]

         [:div.value
          [:div.result current]

          (when-not (= 0 delta)
            [:div.mod delta])]
         ]))))
