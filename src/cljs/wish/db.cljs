(ns wish.db)

(def default-db
  {:page [:home]
   :device-type :default

   ; map of provider id to their current state. Acceptable states:
   ;  - :ready        The provider is ready to be used. Setting this
   ;                  state will trigger a call to `(query-sheets)`
   ;  - :cache-only   The provider could not be initialized, but a
   ;                  cache is available to perform operations
   ;  - :signed-out   The provider is initialized, but not set up.
   ;  - :unavailable  The provider could not be initialized.
   ;  - nil           The provider is still initializing
   :provider-states {}

   ; set of provider ids that are in the process of fetching
   ; their lists of available character sheets
   :providers-listing #{}

   ; hope for the best
   :online? true

   ; map of :id -> {:id,:name,}
   :data-sources {}

   :sheets {}
   :my-sheets #{}

   ; persistent state of sheet filters for browser
   :sheets-filters {:mine? true
                    :shared? false}

   :showing-overlay nil

   ::save-errors #{}
   ::pending-saves #{}
   ::processing-saves #{}})
