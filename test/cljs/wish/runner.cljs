(ns wish.runner
  (:require [doo.runner :refer-macros [doo-all-tests]]
            [wish.events-test]
            [wish.inventory-test]
            [wish.push-test]
            [wish.providers.gdrive.api-test]
            [wish.sheets.compiler-test]
            [wish.sheets.dnd5e.builder-test]
            [wish.sheets.dnd5e.data-test]
            [wish.sheets.dnd5e.events-test]
            [wish.sheets.dnd5e.overlays-test]
            [wish.sheets.dnd5e.overlays.custom-item-test]
            [wish.sheets.dnd5e.subs-test]
            [wish.sheets.dnd5e.subs.base-test]
            [wish.sheets.dnd5e.subs.spells-test]
            [wish.sheets.dnd5e.util-test]
            [wish.util-test]
            [wish.util.formatted-test]
            [wish.views.sheet-builder-util-test]
            [wish.views.widgets.limited-select-test]))

(doo-all-tests #"wish\..*-test")
