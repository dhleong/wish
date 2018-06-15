(ns ^{:author "Daniel Leong"
      :doc "Core types, etc for DataSource"}
  wish.sources.core)

(defprotocol IDataSource
  "Anything that provides features, classes, etc."
  (id [this])
  (find-class [this id])
  (find-race [this id]))

(deftype DataSource [id data]
  IDataSource
  (find-class [this id]
    (get-in (.-data this)
            [:classes id]))

  (find-race [this id]
    (get-in (.-data this)
            [:races id]))

  (id [this]
    (.-id this)))

(defn- first-delegate-by-id
  [^CompositeDataSource s, method id]
  (some #(method % id) (.-delegates s)))

(deftype CompositeDataSource [id delegates]
  IDataSource
  (find-class [this id]
    (first-delegate-by-id this find-class id))
  (find-race [this id]
    (first-delegate-by-id this find-race id))

  (id [this]
    (.-id this)))
