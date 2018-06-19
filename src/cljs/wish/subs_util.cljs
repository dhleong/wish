(ns ^{:author "Daniel Leong"
      :doc "subs-util"}
  wish.subs-util)

(defn active-sheet-id
  [db & [page-vec]]
  (let [page-vec (or page-vec
                     (:page db))]
    (let [[page args] page-vec]
      (when (= :sheet page)
        ; NOTE: the first arg is the sheet kind;
        ; the second is the id
        (second args)))))

