(ns wish.sheets.dnd5e.subs.nav
  (:require [re-frame.core :as rf :refer [reg-sub]]
            [wish.sheets.dnd5e.subs.spells :as spells]))

; ======= 5e-specific nav =================================

(reg-sub :5e/page :5e/page)
(reg-sub :5e/actions-page :5e/actions-page)

(defn- page-specific-sheet
  [[sheet->page sheet-id] [_ default]]
  (get sheet->page sheet-id default))

(reg-sub
  ::page
  :<- [:5e/page]
  :<- [:active-sheet-id]
  :<- [::spells/spellcaster-blocks]
  :<- [:device-type]
  (fn [[_sheet->page _sheet-id
        spell-classes device-type
        :as input] _]
    (let [smartphone? (= :smartphone device-type)
          default (if smartphone?
                    :abilities
                    :actions)
          base (page-specific-sheet input [nil default])]

      ; with keymaps, a user might accidentally go to :spells
      ; but not have spells; in that case, fall back to :actions
      (if-not (or (and (= :abilities base)
                       (not smartphone?))
                  (and (= :spells base)
                       (not (seq spell-classes))))
        ; normal case
        base

        ; fallback
        default))))

(reg-sub
  ::actions-page
  :<- [:5e/actions-page]
  :<- [:active-sheet-id]
  page-specific-sheet)
