(ns wish.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [wish.core-test]
              [wish.sources.compiler-test]
              [wish.templ.fun-test]))

(doo-tests 'wish.core-test
           'wish.sources.compiler-test
           'wish.templ.fun-test)
