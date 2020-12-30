(ns wish.util.limited-use)

(defn restore-trigger-matches?
  [required actual]
  (cond
    (keyword? required) (= required actual)
    (set? required) (contains? required actual)
    (coll? required) (contains? (set required)
                                actual)))

