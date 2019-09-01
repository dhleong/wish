(ns ^{:author "Daniel Leong"
      :doc "Convenience utils"}
  wish.sources.util
  (:require [wish.sources.core :as src]))

(defn call-with [f container]
  (if (fn? f)
    (f container)
    f))

(defn expand-feature [container feature]
  (update feature :desc call-with container))

(defn inflate-feature [^src/DataSource data-source container feature-id]
  (when-let [f (src/find-feature data-source feature-id)]
    (expand-feature container f)))

