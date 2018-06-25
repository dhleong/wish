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
