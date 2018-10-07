(ns ^{:author "Daniel Leong"
      :doc "compiler-specific routines"}
  wish.sources.compiler.util)

(def ^:private sort-reversed (partial sort (fn [a b]
                                             (compare b a))))

(defn combine-sorts
  "Given two entities that might `:wish/sort`,
   combine them into `:wish/sorts` on the first one"
  [a b]
  (cond
    ; easy case
    (and (:wish/sorts a)
         (:wish/sort b))
    (-> a
        (update :wish/sorts conj (:wish/sort b))
        (update :wish/sorts sort-reversed))

    ; easiest case; no new sort to add
    (:wish/sorts a)
    a

    ; no existing :wish/sorts
    (and (:wish/sort a)
         (:wish/sort b))
    (assoc a :wish/sorts (sort-reversed (list (:wish/sort a)
                                              (:wish/sort b))))

    ; single entries
    (:wish/sort a)
    (assoc a :wish/sorts (list (:wish/sort a)))

    (:wish/sort b)
    (assoc a :wish/sorts (list (:wish/sort b)))

    ; just in case:
    :else a))
