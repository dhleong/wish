(ns wish.sheets.dnd5e.subs-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [cljs.nodejs :as node]
            [wish.sheets.dnd5e.subs :refer [level->proficiency-bonus]]))

(deftest level->proficiency-bonus-test
  (testing "Low levels"
    (is (= 2 (level->proficiency-bonus 1)))
    (is (= 2 (level->proficiency-bonus 2)))
    (is (= 2 (level->proficiency-bonus 4))))
  (testing "Higher levels"
    (is (= 3 (level->proficiency-bonus 5)))
    (is (= 3 (level->proficiency-bonus 8)))
    (is (= 4 (level->proficiency-bonus 9)))))

