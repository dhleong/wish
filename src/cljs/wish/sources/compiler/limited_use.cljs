(ns ^{:author "Daniel Leong"
      :doc "Limited-use compiler"}
  wish.sources.compiler.limited-use
  (:require [wish.templ.fun :refer [->callable]]))

(defn compile-limited-use
  "Compile a limited-use map"
  [fm]
  (-> fm
      (update :uses ->callable)
      (update :restore-amount ->callable)))

