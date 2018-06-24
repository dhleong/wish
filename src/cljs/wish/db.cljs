(ns wish.db)

(def default-db
  {:page [:home]
   :provider-states {}

   ; set of provider ids that are in the process of fetching
   ; their lists of available character sheets
   :providers-listing #{}

   :showing-overlay nil

   ::pending-saves #{}
   ::processing-saves #{}})
