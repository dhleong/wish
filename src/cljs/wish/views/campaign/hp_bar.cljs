(ns ^{:author "Daniel Leong"
      :doc "hp-bar"}
  wish.views.campaign.hp-bar
  (:require [garden.color :as color]))

(def ^:private health-colors
  ["#cc4433"
   "#cc6633"
   "#aaaa33"
   "#00cc33"])

(defn- css% [fraction]
  (str (int (* 100 fraction)) "%"))

; NOTE: garden's weighted-mix fn is borked, so let's
; make the real deal:
(defn weighted-mix
  "Returns a hex color that is `weight` % from
   color1 to color2; weight must be a decimal
   in the range `[0, 1]`.
   A weight of 0 means color1 is returned, while
   a weight of 1.0 means color2 is returned."
  [color1 color2 weight]
  (letfn [(mix [a b]
            (int (+ (* a (- 1.0 weight))
                    (* b weight))))]
    (let [rgb1 (color/hex->rgb color1)
          rgb2 (color/hex->rgb color2)]
      (->> [:red :green :blue]
           (reduce
             (fn [m channel]
               (assoc m channel (mix (channel rgb1)
                                     (channel rgb2))))
             {})
           (color/rgb->hex)))))

(defn- perc->color [fraction]
  (let [fractional-index (* (dec (count health-colors))
                            fraction)
        int-val (int fractional-index)
        weight (- fractional-index int-val)]
    (if (<= weight 0.0001)
      (nth health-colors int-val)

      (let [start (nth health-colors int-val)
            end (nth health-colors (inc int-val))]
        (weighted-mix start end weight)))))

(defn hp-bar [hp max-hp]
  (let [perc (/ hp max-hp)]
    [:div.hp-bar
     [:div.bar {:style {:background-color (perc->color perc)
                        :width (css% perc)}}]
     [:div.label
      hp " / " max-hp]
     ]))
