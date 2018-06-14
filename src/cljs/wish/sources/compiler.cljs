(ns ^{:author "Daniel Leong"
      :doc "DataSource compiler"}
  wish.sources.compiler
  (:require [wish.templ.fun :refer [->callable]]))

; ======= features =========================================

(defn compile-max-options
  ":max-options compiles to an acceptor function that
   expects `{:features []}`, where :features is the list of
   features to be limited."
  [o]
  (when o
    (cond
      (number? o) (fn [{:keys [features]}]
                    (<= (count features) o))

      ;; (vector? o) ; TODO support filters list whenever we have it

      (and (list? o)
           (= 'fn (first o))) (->callable o)

      :else #(println "Invalid :max-options " o))))

(defn compile-feature
  "Compile a feature map"
  [fm]
  (-> fm
      (update :max-options compile-max-options)))


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

