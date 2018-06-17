(ns ^{:author "Daniel Leong"
      :doc "compiler.entity"}
  wish.sources.compiler.entity)

(defn compile-entity
  [e]
  (update e :features
          (fn [features]
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
                               (str "Unexpected features: " features))))))))
