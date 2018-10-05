(ns ^{:author "Daniel Leong"
      :doc "compiler.entity"}
  wish.sources.compiler.entity
  (:require [wish.sources.compiler.fun :refer [->callable]]))

(defn- inflate-features
  [features]
  (when features
    (cond
      (vector? features) (reduce-kv
                           (fn [m i feature]
                             (if (map? feature)
                               (assoc m
                                      (:id feature)
                                      (assoc feature :wish/sort [0 i]))
                               (assoc m feature {:wish/sort [0 i]})))
                           {}
                           features)
      (map? features) features
      :else (throw (js/Error.
                     (str "Unexpected features: " features))))))

(defn compile-entity
  [e]
  (-> e
      (update :features inflate-features)
      (update :dice ->callable)))
