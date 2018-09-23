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

(defn- ->indices-set [partial-index]
  #{(int (Math/floor partial-index))
    (int (Math/ceil partial-index))})

(defn swipeable
  [opts & children]
  (r/with-let [visible-indices (r/atom nil)
               pending-index (r/atom nil)
               animate-transitions? (r/atom false)
               set-indicies! (fn [new-indicies]
                               (when-not (= new-indicies @visible-indices)
                                 (reset! visible-indices new-indicies)))]
    (let [children (keep identity children)
          key->index (index-children children)
          index->key (map-invert key->index)
          {:keys [get-key set-key!]} (unpack-key-ops opts)
          view-opts (merge
                      (select-keys opts [:enable-mouse-events])
                      {:animate-transitions @animate-transitions?

                       :index (when get-key
                                (get key->index (get-key)))

                       :on-switching (fn [index type]
                                       (case type
                                         "move"
                                         (do
                                           (when-not @animate-transitions?
                                             (reset! animate-transitions? true))
                                           (set-indicies!  (->indices-set index)))

                                         "end"
                                         (do
                                           (reset! pending-index index)
                                           (set-key! (get index->key index)))))

                       :on-transition-end (fn [& args]
                                            (set-indicies! #{@pending-index}))})
          visible @visible-indices
          visible (if (not (contains? visible (:index view-opts)))
                    #{(:index view-opts)}
                    visible)]

      (into [swipeable-views view-opts]
            (->> children
                 (map-indexed (fn [idx child]
                                (if (contains? visible idx)
                                  child
                                  [:div.placeholder]))))))))
