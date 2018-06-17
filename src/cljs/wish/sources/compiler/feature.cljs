(ns ^{:author "Daniel Leong"
      :doc "Feature compiler"}
  wish.sources.compiler.feature
  (:require [wish.templ.fun :refer [->callable]]))

(defn compile-max-options
  ":max-options compiles to an acceptor function that
   expects `{:features []}`, where :features is the list of
   features to be limited."
  [o]
  (when o
    (cond
      ; already compiled
      (fn? o) o

      (number? o) (fn [{:keys [features]}]
                    (<= (count features) o))

      ;; (vector? o) ; TODO support filters list whenever we have it

      (and (list? o)
           (= 'fn (first o))) (->callable o)

      :else #(println "Invalid :max-options " o))))

(defn compile-feature
  "Compile a feature map"
  [fm]
  ; TODO how can sheets declare keys that should be callable?
  (-> fm
      (update :max-options compile-max-options)
      (update :dice ->callable)))

