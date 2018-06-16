(ns wish.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [wish.core-test]
              [wish.sheets.dnd5e.subs-test]
              [wish.sources.compiler-test]
              [wish.sources.compiler.feature-test]
              [wish.sources.compiler.limited-use-test]
              [wish.templ.fun-test]))

(doo-tests 'wish.core-test
           'wish.sheets.dnd5e.subs-test
           'wish.sources.compiler-test
           'wish.sources.compiler.feature-test
           'wish.sources.compiler.limited-use-test
           'wish.templ.fun-test)
