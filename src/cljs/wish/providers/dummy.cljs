(ns ^{:author "Daniel Leong"
      :doc "Dummy provider"}
  wish.providers.dummy
  (:require [wish.util :refer [>evt]]))

(def ^:private dummy-sheet
  {:v [1 1] ; wish + sheet version numbers
   :updated 1234 ; date
   :id "my-sheet-id"

   :name "Mal Reynolds"

   ; list of ids
   ; NOTE: a lot of these don't make sense but are here as reference
   :sources [:wish/dnd5e-srd]

   ; lists of ids; note that not all systems will support multi class or multi race,
   ; but we'll allow it in the file
   :classes
   {:cleric {:id :cleric
             :level 1
             :primary? true}}

   :races [:human]

   ; map of limited-use ID -> number used
   :limited-uses
   {:paladin/lay-on-hands#uses 0}

   ; map of feature-id -> options chosen
   :options
   {:paladin/oath [:paladin/vengeance]}

   ; opaque, sheet-specific data
   :sheet
   {:abilities
    {:str 12
     :dex 13
     :con 15
     :int 10
     :wis 14
     :cha 8}}

   ; notes is a sequence of [date note] pairs
   :notes
   [[1234 "Note"]]})

(defn init!
  []
  (println "INIT :dummy!")
  (>evt [:put-sheet! :dummy/my-sheet-id dummy-sheet]))
