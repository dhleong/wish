(ns ^{:author "Daniel Leong"
      :doc "swipeable"}
  wish.views.widgets.swipeable
  (:require [clojure.set :refer [map-invert]]
            [reagent.core :as r]
            [cljsjs.react-swipeable-views]
            [wish.util :refer [<sub]]))

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
  "Container that lets users on touchscreen devices swipe between the child views.
   Each child must have a ^{:key}. The currently-displayed page can be controlled
   either by providing `:ratom`, whose value is the key, or `:get-key` and `:set-key`,
   functions which set and get the current key, respectively."
  [opts & children]
  (r/with-let [visible-indices (r/atom nil)
               pending-index (r/atom nil)

               ; animate transitions by default on smartphones
               animate-transitions? (r/atom (= :smartphone (<sub [:device-type])))

               ; because this may be called rapidly as the user swipes, we make sure
               ; to only call reset! if it's actually changed, since otherwise (or
               ; when using swap!) it will trigger a re-render for each pixel the
               ; finger moves
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

                       ; set the current index by getting the current key,
                       ; if provided
                       :index (when get-key
                                (get key->index (get-key)))

                       ; on-switching is called many times as the user swipes
                       :on-switching (fn [index type]
                                       (case type
                                         "move"
                                         (do
                                           ; if we know the user can swipe, we know
                                           ; we want to animate transitions
                                           (when-not @animate-transitions?
                                             (reset! animate-transitions? true))
                                           (set-indicies! (->indices-set index)))

                                         "end"
                                         (do
                                           (reset! pending-index index)
                                           (set-key! (get index->key index)))))

                       ; hide the other view *after* the swipe animation settles
                       :on-transition-end (fn [& _args]
                                            (set-indicies! #{@pending-index}))})

          ; the current index (and, by extension, the visible indicies) can be changed
          ; instantly by tapping, or over time by swiping. This resolves any inconsistencies
          ; that may pop up as a result of this
          visible @visible-indices
          visible (if (not (contains? visible (:index view-opts)))
                    #{(:index view-opts)}
                    visible)]

      ; finally, render the children into a swipeable-views container, replacing
      ; non-visible children with a placeholder div
      (into [swipeable-views view-opts]
            (->> children
                 (map-indexed (fn [idx child]
                                (if (contains? visible idx)
                                  child
                                  [:div.placeholder]))))))))
