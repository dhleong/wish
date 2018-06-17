(ns ^{:author "Daniel Leong"
      :doc "Core types, etc for DataSource"}
  wish.sources.core)

(defprotocol IDataSource
  "Anything that provides features, classes, etc."
  (id [this])
  (expand-list [this id options])
  (find-class [this id])
  (find-feature [this id])
  (find-race [this id]))

(defn- key-by-id
  [^DataSource s, k id]
  (get-in (.-data s)
          [k id]))

(deftype DataSource [id data]
  IDataSource
  (expand-list [this id options]
    (when options
      (println "TODO: filter list " id " by options: " options))
    (key-by-id this :lists id))

  (find-class [this id]
    (key-by-id this :classes id))

  (find-feature [this id]
    (key-by-id this :features id))

  (find-race [this id]
    (key-by-id this :races id))

  (id [this]
    (.-id this)))

(defn- first-delegate-by-id
  [^CompositeDataSource s, method id]
  (some #(method % id) (.-delegates s)))

(deftype CompositeDataSource [id delegates]
  IDataSource
  (expand-list [this id options]
    (mapcat
      #(expand-list % id options)
      (.-delegates this)))
  (find-class [this id]
    (first-delegate-by-id this find-class id))
  (find-feature [this id]
    (first-delegate-by-id this find-feature id))
  (find-race [this id]
    (first-delegate-by-id this find-race id))

  (id [this]
    (.-id this)))
