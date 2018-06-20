(ns wish.db)

(def default-db
  {:page [:home]
   ::pending-saves #{}
   ::processing-saves #{}})
