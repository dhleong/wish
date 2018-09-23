(ns ^{:author "Daniel Leong"
      :doc "swipeable"}
  wish.views.widgets.swipeable
  (:require [reagent.core :as r]
            [cljsjs.react-swipeable-views]))

(def ^:private swipeable-views (r/adapt-react-class js/SwipeableViews))

(defn swipeable
  [opts & children]
  (let [view-opts {:enable-mouse-events true}]
    (into [swipeable-views view-opts]
          children)))
