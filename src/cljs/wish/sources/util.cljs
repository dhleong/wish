(ns ^{:author "Daniel Leong"
      :doc "Convenience utils"}
  wish.sources.util)

(defn call-with [f container]
  (if (fn? f)
    (f container)
    f))

(defn expand-feature [container feature]
  (update feature :desc call-with container))

(defn inflate-feature [data-source container feature-id]
  (when-let [f (get-in data-source [:features feature-id])]
    (expand-feature container f)))

