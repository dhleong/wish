(ns ^{:author "Daniel Leong"
      :doc "swipeable"}
  wish.views.widgets.swipeable
  (:require [clojure.set :refer [map-invert]]
            [reagent.core :as r]
            [cljsjs.react-swipeable-views]))

(def ^:private swipeable-views (r/adapt-react-class js/SwipeableViews))

(defn- index-children
  [children]
  (->> children
       (map-indexed list)
       (reduce
         (fn [m [index c]]
           (assoc m (-> c meta :key) index))
         {})))

(defn- unpack-key-ops
  "Given a map of options to swipeable, return a map with
   :get-key, a fn to retrieve the current key, and :set-key!, a
   function to write a new key"
  [opts]
  (if-let [ratom (:ratom opts)]
    {:get-key #(deref ratom)
     :set-key! #(reset! ratom %)}

    (let [{:keys [get-key set-key!]} opts]
      (when (and get-key set-key!)
        {:get-key get-key
         :set-key! set-key!}))))

(defn swipeable
  [opts & children]
  (let [children (keep identity children)
        key->index (index-children children)
        index->key (map-invert key->index)
        {:keys [get-key set-key!]} (unpack-key-ops opts)
        view-opts {:enable-mouse-events false
                   :index (when get-key
                            (get key->index (get-key)))
                   :on-change-index (when set-key!
                                      (fn [new-index]
                                        (set-key! (get index->key new-index))))}]
    (into [swipeable-views view-opts]
          children)))
