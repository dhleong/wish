(ns ^{:author "Daniel Leong"
      :doc "util"}
  wish.util
  (:require-macros [wish.util :refer [fn-click]])
  (:require [re-frame.core :refer [subscribe dispatch]]))

(def >evt dispatch)
;; (def <sub (comp deref subscribe))
(defn <sub [query]
  (try @(subscribe query)
       (catch :default e
         (throw (ex-info (str "ERROR deref'ing " query)
                         {:error e})))))

(defn distinct-by
  ([f]
   ; transducer implementation based on core/distinct
   (fn [rf]
     (let [seen (volatile! #{})]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (let [k (f input)]
            (if (contains? @seen k)
              result
              (do (vswap! seen conj k)
                  (rf result input)))))))))
  ([f coll]
   (let [step (fn step [xs seen]
                (lazy-seq
                  ((fn [[x :as xs] seen]
                     (when-let [s (seq xs)]
                       (let [fx (f x)]
                         (if (contains? seen fx)
                           (recur (rest s) seen)
                           (cons x (step (rest s) (conj seen fx)))))))
                   xs seen)))]
     (step coll #{}))))

(defn dec-dissoc
  "Update the key `k` in the given map `m`, decrementing it
   if > 1 or dissoc if <= 1"
  [m k]
  (if (> (get m k) 1)
    (update m k dec)
    (dissoc m k)))

(defn update-dissoc
  "Like update, but dissoc's the key if the new value is nil"
  [m k f x]
  (if-some [new-value (f (get m k) x)]
    (assoc m k new-value)
    (dissoc m k)))

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

(defn inc-or
  "Like (inc), but instead of defaulting to 1 when
   the existing value is nil, returns `default`"
  [v default]
  (if v
    (inc v)
    default))

(defn update-each-value
  "Applies f to the value of each key in m"
  [m f]
  (reduce-kv
    (fn [m k v]
      (assoc m k (f v)))
    m
    m))

(defn toggle-in
  "Like assoc-in, but if the existing value at the given path
   matches the provided value, it is set to nil instead"
  [m path v]
  (update-in m path (fn [old]
                      (when-not (= old v)
                        v))))

(defn padded-compare
  "Given two vectors of numbers, this will compare them as if the
   shorter were padded with zeroes to be the same length as the
   longer. This allows for orderings like:

    [0 0] [0 0 0] [4 0] [4 0 0]"
  [a b]
  (let [longest (max (count a)
                     (count b))]
    (if (= 0 longest)
      0 ; quick reject

      (loop [i 0]
        (let [c (compare (nth a i 0)
                         (nth b i 0))
              nexti (inc i)]
          (if (and (zero? c) (< nexti longest))
            (recur nexti)
            c))))))

(defn click>evts
  "Returns an on-click handler that dispatches the given events
   and prevents the default on-click events"
  [& events]
  (fn-click [e]
    (doseq [event events]
      (when event
        (>evt event)))

    ; always prevent propagation
    (.stopPropagation e)))

(defn click>evt
  "Returns an on-click handler that dispatches the given event
   and prevents the default on-click events"
  [event & {:keys [propagate?]
            :or {propagate? true}}]
  (fn-click [e]
    (>evt event)

    ; prevent propagation, optionally
    (when-not propagate?
      (.stopPropagation e))))

(defn click>reset!
  "Returns an on-click handler that performs (reset!) with
   the given arguments"
  [a v]
  (fn-click
    (reset! a v)))

(defn click>swap!
  "Returns an on-click handler that performs (swap!) with the
   given arguments"
  ([a f]
   (fn-click
     (swap! a f)))
  ([a f x]
   (fn-click
     (swap! a f x)))
  ([a f x y]
   (fn-click
     (swap! a f x y))))

(def is-safari?
  (memoize
    (fn is-safari? []
      (and (boolean js/navigator.vendor)
           (re-find #"Apple" js/navigator.vendor)))))

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
  (let [context (or (:wish/context entity)
                    entity)
        f (k entity)]
    (if-not (fn? f) f  ; static value
      (try
        (f (apply assoc context extra-kvs))
        (catch js/Error e
          (throw (ex-info (str "Error invoking " k "(" f ") on "
                               (or (when-let [id (:id context)]
                                     (str "{:id " id " ...}"))
                                   context))
                          {}
                          e)))))))

(defn assoc-by-id
  ([] {})
  ([m] m)
  ([m entity]
   (assoc m (:id entity) entity)))

(defn ->map
  "Given a seq of entities, return a map of :id -> entity"
  [entities]
  (reduce
    assoc-by-id
    {}
    entities))

(defn ->set
  "Given a collection, turn it into a set (if it isn't already)"
  [coll]
  (if (set? coll)
    coll
    (set coll)))

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

(defn conj-class
  "Expected to be used as, eg:

    (update opts :class conj-class extra-class)

   where `opts` is an element options map possibly containing
   the key `:class`, which can be missing, a vector of class names,
   or a scalar of a single class name."
  [old new-class]
  (cond
    (coll? old)
    (cons new-class old)

    old
    [new-class old]

    :else [new-class]))
