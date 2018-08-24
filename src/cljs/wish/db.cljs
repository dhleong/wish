(ns wish.db)

(def default-db
  {:page [:home]
   :provider-states {}

   ; map of :id -> {:id,:name,}
   :data-sources {}

   :sheets {}
   :my-sheets #{}

   ; persistent state of sheet filters for browser
   :sheets-filters {:mine? true
                    :shared? false}

   ; set of provider ids that are in the process of fetching
   ; their lists of available character sheets
   :providers-listing #{}

   :showing-overlay nil

   ::save-errors #{}
   ::pending-saves #{}
   ::processing-saves #{}})
