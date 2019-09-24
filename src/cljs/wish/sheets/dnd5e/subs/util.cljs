(ns wish.sheets.dnd5e.subs.util
  (:require [clojure.string :as str]
            [wish-engine.core :as engine]
            [wish.sources.util :as src-util]
            [wish.util.string :as wstr]
            [wish.util :refer [->set]]))

(defn filter-by-str
  "Filter's by :name using the given str"
  [filter-str coll]
  (if-not (str/blank? filter-str)
    (->> coll
         (filter (fn [{n :name}]
                   (wstr/includes-any-case? n filter-str))))
    coll))

(defn feature-by-id
  ([container feature-id]
   (or (get-in container [:features feature-id])
       (get-in container [:list-entities feature-id])))
  ([data-source container feature-id]
   (or (feature-by-id container feature-id)
       (feature-by-id data-source feature-id)
       (src-util/inflate-feature data-source container feature-id))))

(defn feature-in-lists [engine-state entity-lists id]
  (or (feature-by-id engine-state id)
      (some (fn [source]
              (get-in source [:features id]))
            (flatten entity-lists))))

(defn options-of-list
  [engine-state list-id options-set]
  (->> (engine/inflate-list engine-state list-id)
       (filter (comp (->set options-set) :id))))
