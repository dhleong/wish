(ns wish.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [wish.events-test]
              [wish.sheets.dnd5e.subs-test]
              [wish.sheets.dnd5e.util-test]
              [wish.sources.compiler-test]
              [wish.sources.compiler.feature-test]
              [wish.sources.compiler.limited-use-test]
              [wish.sources.compiler.lists-test]
              [wish.templ.fun-test]
              [wish.util-test]))

(doo-tests 'wish.events-test
           'wish.sheets.dnd5e.subs-test
           'wish.sheets.dnd5e.util-test
           'wish.sources.compiler-test
           'wish.sources.compiler.feature-test
           'wish.sources.compiler.limited-use-test
           'wish.sources.compiler.lists-test
           'wish.templ.fun-test
           'wish.util-test)
