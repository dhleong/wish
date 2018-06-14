(ns ^{:author "Daniel Leong"
      :doc "Core types, etc for DataSource"}
  wish.sources.core)

(defprotocol IDataSource
  "Anything that provides features, classes, etc."
  (find-class [this id]))

(deftype DataSource [id data]
  IDataSource
  (find-class [this id]
    nil))

(deftype CompositeDataSource [id delegates]
  IDataSource
  (find-class [this id]
    (some find-class (.-delegates this))))
