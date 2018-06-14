(ns ^{:author "Daniel Leong"
      :doc "DataSource compiler"}
  wish.sources.compiler
  (:require [wish.sources.compiler.feature :refer [compile-feature]]
            [wish.templ.fun :refer [->callable]]))

; ======= directives =======================================

(def directives
  {:!provide-feature
   (fn provide-feature [state & args]
     (let [feature-maps (filter map? args)
           filters (filter vector? args)]
       ; TODO filters?
       (->> feature-maps
            (map compile-feature)
            (reduce
              (fn [s feature-map]
                (update s :features
                        assoc
                        (:id feature-map) feature-map))
              state))))
   })

(defn apply-directive [state directive-vector]
  (let [[kind & args] directive-vector]
    (if-let [f (get directives kind)]
      ; valid directive
      (apply f state args)

      ; unknown; ignore
      (do
        (println "WARN: unknown directive:" kind)
        state))))


; ======= public api =======================================

(defn compile-directives
  "Given a sequence of directives, return a compiled DataSource state"
  [directives]
  (reduce
    apply-directive
    {}
    directives))

