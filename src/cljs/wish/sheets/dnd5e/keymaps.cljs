(ns ^{:author "Daniel Leong"
      :doc "keymaps"}
  wish.sheets.dnd5e.keymaps
  (:require [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.overlays :as overlays]))

(def ^:private event-keys
  [;; hide any open overlay
   [[:toggle-overlay nil]
    ; via esc:
    [{:which 27}]]

   ;; open notes
   [[:toggle-overlay [#'overlays/notes-overlay]]

    ; via:
    [; press `n`
     {:which 78}]]

   ;; open health management
   [[:toggle-overlay [#'overlays/hp-overlay]]

    ; via:
    [; press `h`
     {:which 72
      :ctrlKey false}]]

   ;; show "actions"
   [[::events/page! :actions]

    ; via:
    [; press `a`
     {:which 65}]]

   ;; show "spells"
   [[::events/page! :spells]

    ; via:
    [; press `a`
     {:which 83}]]

   ;; show "inventory"
   [[::events/page! :inventory]

    ; via:
    [; press `i`
     {:which 73}]]

   ;; show "features"
   [[::events/page! :features]

    ; via:
    [; press `f`
     {:which 70}]]
   ])

(def maps {:event-keys event-keys})
