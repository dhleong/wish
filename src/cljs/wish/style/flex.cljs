(ns ^{:author "Daniel Leong"
      :doc "Flexbox style defs for inline use"}
  wish.style.flex)

; TODO add vendor fallbacks
(def flex {:display 'flex})
(def wrap {:flex-wrap 'wrap})
(def vertical (merge
                flex
                {:flex-direction 'column}))

(def grow {:flex-grow 1})

(def align-center {:align-items 'center})

(def justify-center {:justify-content 'center})


(def center (merge flex
                   align-center))
(def vertical-center (merge vertical
                            justify-center))

(defmulti apply-flex-opt (fn [_m opt] (key opt)))
(defmethod apply-flex-opt :flow
  [_ [_ v]]
  (case v
    :vertical vertical
    nil))
(defmethod apply-flex-opt :center
  [m [_ v]]
  (case v
    :both (merge justify-center align-center)
    :horizontal (if (= :vertical (:flow m))
                  align-center
                  justify-center)
    :vertical (if (= :vertical (:flow m))
                justify-center
                align-center)))
(defmethod apply-flex-opt :wrap?
  [_ [_ v]]
  (when v
    wrap))

(defn create [& options]
  (let [opts (apply hash-map (remove map? options))
        maps (->> options
                  (filter map?)
                  (apply merge))]
    (->> opts
         (reduce
           (fn [m opt]
             (if-let [applied (apply-flex-opt opts opt)]
               (merge m applied)
               m))
           {})
         (apply merge flex maps))))
