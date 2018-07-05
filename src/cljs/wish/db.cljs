(ns wish.db)

(def default-db
  {:page [:home]
   :provider-states {}

   ; map of :id -> {:id,:name,}
   :data-sources {}

   :sheets {}

   ; set of provider ids that are in the process of fetching
   ; their lists of available character sheets
   :providers-listing #{}

   :showing-overlay nil

   ::save-errors #{}
   ::pending-saves #{}
   ::processing-saves #{}})
