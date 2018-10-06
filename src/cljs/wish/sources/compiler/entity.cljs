(ns ^{:author "Daniel Leong"
      :doc "compiler.entity"}
  wish.sources.compiler.entity
  (:require [wish.sources.compiler.fun :refer [->callable]]))

(defn- update-each-key
  [m f]
  (reduce-kv
    (fn [m k v]
      (assoc m k (f v k)))
    m
    m))

(defn- inflate-features
  [features level]
  (when features
    (cond
      (vector? features) (reduce-kv
                           (fn [m i feature]
                             (if (map? feature)
                               (assoc m
                                      (:id feature)
                                      (assoc feature :wish/sort [level i]))
                               (assoc m feature {:wish/sort [level i]})))
                           {}
                           features)
      (map? features) features
      :else (throw (js/Error.
                     (str "Unexpected features: " features))))))

(defn- inflate-+features
  [levels-map level]
  (cond-> levels-map
    (:+features levels-map)
    (update :+features inflate-features level)))

(defn compile-entity
  [e]
  (-> e
      (update :features inflate-features 0)
      (update :levels update-each-key inflate-+features)
      (update :&levels update-each-key inflate-+features)
      (update :dice ->callable)))
