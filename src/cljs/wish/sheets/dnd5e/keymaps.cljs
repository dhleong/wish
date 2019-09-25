(ns ^{:author "Daniel Leong"
      :doc "keymaps"}
  wish.sheets.dnd5e.keymaps
  (:require [wish.sheets.dnd5e.events :as events]
            [wish.sheets.dnd5e.overlays :as overlays]
            [wish.sheets.dnd5e.overlays.hp :as hp]))

(def ^:private event-keys
  [;; hide any open overlay
   [[:toggle-overlay nil]
    ; via esc:
    [{:keyCode 27}]]

   ;; open notes
   [[:toggle-overlay [#'overlays/notes-overlay]]

    ; via:
    [; press `n`
     {:keyCode 78}]]

   ;; open health management
   [[:toggle-overlay [#'hp/overlay]]

    ; via:
    [; press `h`
     {:keyCode 72
      :ctrlKey false}]]

   ;; show "actions"
   [[::events/page! :actions]

    ; via:
    [; press `a`
     {:keyCode 65}]]

   ;; show "spells"
   [[::events/page! :spells]

    ; via:
    [; press `a`
     {:keyCode 83}]]

   ;; show "inventory"
   [[::events/page! :inventory]

    ; via:
    [; press `i`
     {:keyCode 73}]]

   ;; show "features"
   [[::events/page! :features]

    ; via:
    [; press `f`
     {:keyCode 70}]]
   ])

(def maps {:event-keys event-keys})
