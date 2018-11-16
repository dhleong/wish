(ns ^{:author "Daniel Leong"
      :doc "subs-util"}
  wish.subs-util)

(defn active-sheet-id
  [db & [page-vec]]
  (let [page-vec (or page-vec
                     (:page db))]
    (let [[page args] page-vec]
      (case page
        :campaign (first args)
        :sheet args
        :sheet-builder (first args)

        ; else, no sheet
        nil))))

