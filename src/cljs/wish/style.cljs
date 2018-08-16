(ns ^{:author "Daniel Leong"
      :doc "style"}
  wish.style
  (:require [wish.config :refer [server-root]]))

(defn- asset [n]
  (str server-root "/assets/" n))

