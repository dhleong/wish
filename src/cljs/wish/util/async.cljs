(ns ^{:author "Daniel Leong"
      :doc "async"}
  wish.util.async
  (:require [cljs.core.async :refer [chan put!]]))

(defn promise->chan
  "Convert a promise into a channel in the normal way.
   A buffer + transducer may optionally be provided
   as per the (chan) function"
  ([p]
   (promise->chan p nil nil))
  ([p buf-or-size xform]
   (let [ch (if (and buf-or-size xform)
              (chan buf-or-size xform)
              (chan))]
     (.then
       p
       (fn [resp]
         (put! ch [nil resp]))
       (fn [e]
         (put! ch [e nil])))
     ch)))
