(ns ^{:author "Daniel Leong"
      :doc "async"}
  wish.util.async
  (:require [cljs.core.async :refer [chan put!]]))

(defn promise->chan
  "Convert a promise into a channel in the normal way"
  [p]
  (let [ch (chan)]
    (.then
      p
      (fn [resp]
        (put! ch [nil resp]))
      (fn [e]
        (put! ch [e nil])))
    ch))
