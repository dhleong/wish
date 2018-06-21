(ns ^{:author "Daniel Leong"
      :doc "subs-util"}
  wish.subs-util)

(defn active-sheet-id
  [db & [page-vec]]
  (let [page-vec (or page-vec
                     (:page db))]
    (let [[page sheet-id] page-vec]
      (when (= :sheet page)
        sheet-id))))

