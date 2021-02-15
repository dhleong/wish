(ns wish.util.collections
  "(New) Home of collection manipulation fns")

(defn index-where
  "Given an ordered collection, return the index of the first item that
   matches the given predicate. Returns nil if no item matches"
  [coll pred]
  (loop [i 0
         coll coll]
    (when (seq coll)
      (if (pred (first coll))
        i
        (recur
          (inc i)
          (next coll))))))

(defn disj-by
  "Given a collection, return a new collection of the same time that
   does not include the first item matching the given predicate function.
   Returns the original collection if pred does not match any items.
   Currently supports vectors only"
  [coll pred]
  (when coll
    (cond
      (vector? coll) (when-let [index (index-where coll pred)]
                       (into (subvec coll 0 index)
                             (subvec coll (inc index))))

      :else (throw (ex-info "Unsupported coll" {:coll coll})))))
