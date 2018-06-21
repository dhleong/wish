(ns ^{:author "Daniel Leong"
      :doc "compiler.entity"}
  wish.sources.compiler.entity
  (:require [wish.sources.compiler.fun :refer [->callable]]))

(defn- inflate-features
  [features]
  (when features
    (cond
      (vector? features) (reduce
                           (fn [m feature]
                             (if (map? feature)
                               (assoc m (:id feature) feature)
                               (assoc m feature true)))
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
