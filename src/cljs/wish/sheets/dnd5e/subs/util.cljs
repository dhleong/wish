(ns wish.sheets.dnd5e.subs.util
  (:require [clojure.string :as str]
            [wish-engine.core :as engine]
            [wish.sheets.util :as sheet-util]
            [wish.subs-util :refer [reg-id-sub]]
            [wish.util.string :as wstr]
            [wish.util :refer [->set]]))

(defn reg-sheet-sub
  "Convenience for creating a sub that just gets a specific
   field from the :sheet key of the sheet-meta"
  [id getter]
  (reg-id-sub
    id
    :<- [:meta/sheet]
    (fn [sheet _]
      (getter sheet))))

(defn filter-by-str
  "Filter's by :name using the given str"
  [filter-str coll]
  (if-not (str/blank? filter-str)
    (->> coll
         (filter (fn [{n :name}]
                   (wstr/includes-any-case? n filter-str))))
    coll))

; TODO migrate instead of aliasing, probably
(def feature-by-id sheet-util/feature-by-id)

(defn feature-in-lists [engine-state entity-lists id]
  (or (feature-by-id engine-state id)
      (some (fn [source]
              (get-in source [:features id]))
            (flatten entity-lists))))

(defn options-of-list
  [engine-state list-id options-set]
  (->> (engine/inflate-list engine-state list-id)
       (filter (comp (->set options-set) :id))))

(defn- compute-buff [entity buff-entry]
  (if (fn? buff-entry)
    (buff-entry entity)
    buff-entry))

(defn compute-buffs [entity buffs-map]
  (reduce (fn [total b]
            (+ total (compute-buff entity b)))
          0
          (vals buffs-map)))
