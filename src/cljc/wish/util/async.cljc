(ns ^{:author "Daniel Leong"
      :doc "Async util macros"}
  wish.util.async
  (:require [clojure.core.async :refer [go chan put!]]))

(defmacro call-with-cb->chan
  "Given a call like (.execute cli arg), insert a function
   as the last argument that expects a single `response`
   parameter that might contain a .-error property, and
   return a channel that emits [err resp]."
  [body]
  `(let [~'ch (chan)]
     (~@(concat
          body
          [`(fn [resp#]
              (if-let [err# (.-error resp#)]
                (put! ~'ch [err#])
                (put! ~'ch [nil (cljs.core/js->clj resp#
                                        :keywordize-keys true)])))]))
     ~'ch))
