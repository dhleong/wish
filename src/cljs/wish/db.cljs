(ns wish.db)

(def default-db
  {:page [:home]
   :provider-states {}
   ::pending-saves #{}
   ::processing-saves #{}})
