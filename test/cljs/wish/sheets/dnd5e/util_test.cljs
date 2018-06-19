(ns wish.sheets.dnd5e.util-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sheets.dnd5e.util :refer [post-process]]))

(deftest post-process-test
  (testing "Install spell slot limited-uses"
    (let [original {:attrs {:5e/spellcaster {}}}
          processed (post-process original nil :class)]
      (is (contains? (:limited-uses processed)
                     :slots/level-1))
      (is (= 4 ((-> processed :limited-uses
                    :slots/level-1
                    :restore-amount)
                {:used 4}))))))

