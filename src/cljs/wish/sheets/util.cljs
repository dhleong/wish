(ns ^{:author "Daniel Leong"
      :doc "Sheet-related utils"}
  wish.sheets.util
  (:require [clojure.string :as str]
            [wish.subs-util :refer [active-sheet-id]]))

(defn unpack-id
  "Unpack a sheet id into its provider and
   provider-specific id ('pro-sheet-id')"
  [sheet-id]
  (let [k (if (keyword? sheet-id)
            sheet-id
            (keyword sheet-id))]
    [(namespace k)
     (name k)]))

(defn make-id
  "Pack a provider id and a provider-specific id
   ('pro-sheet-id') into a sheet-id"
  [provider-id pro-sheet-id]
  (keyword (if (keyword? provider-id)
             (name provider-id)
             (str provider-id))
           pro-sheet-id))

(defn update-sheet
  [db f & args]
  (let [sheet-id (active-sheet-id db)]
    (apply update-in db [:sheets sheet-id :sheet] f args)))

(defn update-uses
  [db use-id f & args]
  (let [sheet-id (active-sheet-id db)]
    (apply update-in db [:sheets sheet-id :limited-uses use-id] f args)))
