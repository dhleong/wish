(ns ^{:author "Daniel Leong"
      :doc "util"}
  wish.util
  (:require [reagent.dom :as reagent-dom]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]))

(def <sub (comp deref subscribe))
(def >evt dispatch)

(defn deep-merge
  [v & vs]
  (letfn [(rec-merge [a b]
            (cond
              (and (map? a)
                   (map? b))
              (merge-with deep-merge a b)

              ;; (and (sequential? a)
              ;;      (sequential? b))
              ;; (concat a b)

              :else b))]
    (if (some identity vs)
      (reduce #(rec-merge %1 %2) v vs)
      v)))

(defn click>evts
  "Returns an on-click handler that dispatches the given events
   and prevents the default on-click events"
  [& events]
  (fn [e]
    (.preventDefault e)

    (doseq [event events]
      (>evt event))

    ; always prevent propagation
    (.stopPropagation e)))

(defn click>evt
  "Returns an on-click handler that dispatches the given event
   and prevents the default on-click events"
  [event & {:keys [propagate?]
            :or {propagate? true}}]
  (fn [e]
    (.preventDefault e)

    (>evt event)

    ; prevent propagation, optionally
    (when-not propagate?
      (.stopPropagation e))))

(def is-ios?
  (memoize
    (fn is-ios? []
      (and (boolean js/navigator.platform)
           (re-find #"iPad|iPhone|iPod" js/navigator.platform)))))

(defn invoke-callable
  "Invoke a callable in the context of the given entity,
   optionally providing other key-value pairs. Restoring
   a limited-use, for example might look like:

    (call item :restore-amount
      :trigger :long-rest
      :used (get (<sub [:limited-used]) (:id item))

   If the entity has the key :wish/context, that will be
   used as the context instead of the entity itself
   "
  [entity k & extra-kvs]
  (try
    (let [context (or (:wish/context entity)
                      entity)]
      ((k entity) (apply assoc context extra-kvs)))
    (catch js/Error e
      (throw (js/Error. (str "Error invoking " k " on " entity "\n\nOriginal " e))))))

(defn ->map
  "Given a seq of entities, return a map of :id -> entity"
  [entities]
  (zipmap
    (map :id entities)
    entities))

(defn ->set
  "Given a collection, turn it into a set (if it isn't already)"
  [coll]
  (if (set? coll)
    coll
    (set coll)))

(defn navigate!
  [& args]
  (let [evt (vec (cons :navigate! args))]
    (if (is-ios?)
      ; NOTE: on iOS we do some whacky shit to prevent awful flashes
      ;  when swiping back. hopefully there's a more efficient way
      ;  to do this, but for now... this works
      (do
        (dispatch-sync evt)
        (reagent-dom/force-update-all))

      ; When we don't have to worry about back-swipe, we can be more
      ;  relaxed about handling navigation
      (dispatch evt))))

(defn process-map
  "Call (processor s v) for each value in the map
   with key `k` in the state `s`"
  [k processor s]
  (update s k
          (fn [the-map]
            (reduce-kv
              (fn [result k v]
                (assoc result
                       k
                       (processor s v)))
              {}
              the-map))))

