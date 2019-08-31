(ns ^{:author "Daniel Leong"
      :doc "Sheet-related utils"}
  wish.sheets.util
  (:require [wish.subs-util :refer [active-sheet-id]]))

(defn unpack-id
  "Unpack a sheet id into its provider and
   provider-specific id ('pro-sheet-id')"
  [sheet-id]
  (let [k (if (keyword? sheet-id)
            sheet-id
            (keyword sheet-id))]
    [(keyword (namespace k))

     ; NOTE: as below, provider sheet id is prefied with K to safely handle
     ; provider-specific IDs that start with a number
     (subs (name k) 1)]))

(defn make-id
  "Pack a provider id and a provider-specific id
   ('pro-sheet-id') into a sheet-id"
  [provider-id pro-sheet-id]
  (keyword (if (keyword? provider-id)
             (name provider-id)
             (str provider-id))

           ; NOTE we now ALWAYS prefix the provider-specific id with `w`
           ; to ensure we generate valid keywords if the provider sheet
           ; id starts with a number
           (str "w" pro-sheet-id)))


; ======= Sheet-modification utils =========================
; These functions act as -fx event handlers, accepting the
; cofx map, performing the modification described, and scheduling
; a save of the sheet.

(defn update-sheet-path
  "This poorly-named method updates a path in the current *sheet-meta map*,
   and is the basis for other update-* methods below. We should probably
   rename it to update-meta-path or something, so it's clearer that the
   references to 'sheet' in the other method names refer to the :sheet
   key inside a sheet-meta map."
  [{:keys [db]} path f & args]
  (when-let [sheet-id (active-sheet-id db)]
    (let [new-db (apply update-in db
                        (concat [:sheets sheet-id]
                                path)
                        f
                        args)

          ; don't bother scheduling a save if the sheet didn't
          ; actually change
          sheet-changed? (not=
                           (get-in db [:sheets sheet-id])
                           (get-in new-db [:sheets sheet-id]))]
      {:db new-db
       :schedule-save (when sheet-changed?
                        sheet-id)})))

(defn update-sheet
  "Update the sheet-specific map `:sheet`"
  [cofx f & args]
  (apply update-sheet-path cofx [:sheet] f args))

(defn update-in-sheet
  "Update a path in the sheet-specific map `:sheet`"
  [cofx path f & args]
  (apply update-sheet-path cofx (concat [:sheet] path) f args))

(defn update-uses
  "Update the uses count for the given use-id"
  [cofx use-id f & args]
  (apply update-sheet-path cofx [:limited-uses use-id] f args))

(defn get-uses
  [{:keys [db]} use-id]
  (when-let [sheet-id (active-sheet-id db)]
    (get-in db [:sheets sheet-id :limited-uses use-id])))
