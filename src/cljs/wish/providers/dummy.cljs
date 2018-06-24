(ns ^{:author "Daniel Leong"
      :doc "Dummy provider"}
  wish.providers.dummy
  (:require [clojure.core.async :refer [to-chan]]
            [wish.providers.core :refer [IProvider]]
            [wish.util :refer [>evt]]))

(def ^:private dummy-sheet
  {:v [1 1] ; wish + sheet version numbers
   :updated 1234 ; date
   :kind :dnd5e

   :name "Mal Reynolds"

   ; list of ids
   ; NOTE: a lot of these don't make sense but are here as reference
   :sources [:wish/dnd5e-srd]

   ; lists of ids; note that not all systems will support multi class or multi race,
   ; but we'll allow it in the file
   :classes
   {:cleric {:id :cleric
             :level 2
             :primary? true}
    :warlock {:id :warlock
              :level 1}}

   :races [:human]

   ; map of limited-use ID -> number used
   :limited-uses
   {:paladin/lay-on-hands#uses 0
    :slots/level-1 1}

   ; map of feature-id -> options chosen
   :options
   {:cleric/skill-proficiencies [:proficiency/history :proficiency/insight]
    :cleric/domain [:cleric/life-domain]}

   ; opaque, sheet-specific data
   :sheet
   {:abilities
    {:str 12
     :dex 13
     :con 15
     :int 10
     :wis 14
     :cha 8}

    ; vector of rolled HP at level `i-1` for each class
    ; level 1 is normally the max value, but DMs might house rule,
    ; so we'll just let it be whatever you like
    :hp-rolled
    {:cleric [8]
     :warlock [6]}}

   ; notes is a sequence of [date note] pairs
   :notes
   [[1234 "Note"]]})

(deftype DummyProvider []
  IProvider
  (id [this] :dummy)
  (init! [this]
    (println "INIT :dummy!")
    (>evt [:put-sheet! :dummy/my-sheet-id dummy-sheet]))
  (load-raw
    [this id]
    (to-chan [[(js/Error. "Not implemented") nil]]))
  (load-sheet
    [this id]
    (to-chan [[(js/Error. "Not implemented") nil]]))
  (save-sheet
    [this id data]
    (println "Dummy save: " id)
    (to-chan [[nil]])))

(defn create-provider
  []
  (->DummyProvider))
