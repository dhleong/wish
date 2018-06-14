(ns ^{:author "Daniel Leong"
      :doc "Dummy provider"}
  wish.providers.dummy
  (:require [wish.util :refer [>evt]]))

(def ^:private dummy-sheet
  {:v [1 1] ; wish + sheet version numbers
   :updated 1234 ; date
   :id "my-sheet-id"

   ; lists of ids; note that not all systems will support multi class or multi race,
   ; but we'll allow it in the file
   :classes []
   :races []

   ; map of limited-use ID -> number used
   :limited-uses
   {:paladin/lay-on-hands#uses 0}

   ; map of feature-id -> options chosen
   :options
   {:paladin/oath [:paladin/vengeance]}

   ; opaque, sheet-specific data
   :sheet
   {}

   ; notes is a sequence of [date note] pairs
   :notes
   [[1234 "Note"]]})

(defn init!
  []
  (println "INIT :dummy!")
  (>evt [:put-sheet! :dummy-my-sheet-id dummy-sheet]))
