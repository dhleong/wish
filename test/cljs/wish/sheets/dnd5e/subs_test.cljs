(ns wish.sheets.dnd5e.subs-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sheets.dnd5e.subs :refer [level->proficiency-bonus
                                            spell-slots]]))

(deftest level->proficiency-bonus-test
  (testing "Low levels"
    (is (= 2 (level->proficiency-bonus 1)))
    (is (= 2 (level->proficiency-bonus 2)))
    (is (= 2 (level->proficiency-bonus 4))))
  (testing "Higher levels"
    (is (= 3 (level->proficiency-bonus 5)))
    (is (= 3 (level->proficiency-bonus 8)))
    (is (= 4 (level->proficiency-bonus 9)))))

(deftest spell-slots-test
  (testing "Single class, standard"
    (is (= {1 4, 2 3, 3 3, 4 1}
           (spell-slots [{:level 7}]))))
  (testing "Single class, half"
    (is (= {1 4, 2 3}
           (spell-slots [{:level 7
                          :attrs
                          {:5e/spellcaster
                           {:slots :standard/half}}}]))))
  (testing "Multiclass, both standard"
    (is (= {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1}
           (spell-slots [{:level 7}
                         {:level 7}]))))
  (testing "Multiclass, one half"
    (is (= {1 4, 2 3, 3 3, 4 3, 5 2}
           (spell-slots [{:level 7}
                         {:level 7
                          :attrs
                          {:5e/spellcaster
                           {:slots :standard/half
                            :multiclass-levels-mod 2}}}])))))
