(ns wish.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [wish.core-test]))

(doo-tests 'wish.core-test)
