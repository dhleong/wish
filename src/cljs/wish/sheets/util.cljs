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


; ======= Sheet-modification utils =========================
; These functions act as -fx event handlers, accepting the
; cofx map, performing the modification described, and scheduling
; a save of the sheet.

(defn update-sheet-path
  "Update a path in the current sheet meta"
  [{:keys [db]} path f & args]
  (let [sheet-id (active-sheet-id db)]
    {:db (apply update-in db
                (concat [:sheets sheet-id]
                        path)
                f
                args)
     :schedule-save sheet-id}))

(defn update-sheet
  "Update the sheet-specific map `:sheet`"
  [cofx f & args]
  (apply update-sheet-path cofx [:sheet] f args))

(defn update-uses
  "Update the uses count for the given use-id"
  [cofx use-id f & args]
  (apply update-sheet-path cofx [:limited-uses use-id] f args))
