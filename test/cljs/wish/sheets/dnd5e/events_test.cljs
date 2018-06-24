(ns wish.sheets.dnd5e.events-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sheets.dnd5e.events :refer [update-hp-rolled]]))

(deftest update-hp-rolled-test
  (testing "Existing vector + index"
    (is (= {:rogue [42]}
           (update-hp-rolled
             {:rogue [2]}
             [:rogue 0]
             42))))
  (testing "Existing vector, new index"
    (is (= {:rogue [2 42]}
           (update-hp-rolled
             {:rogue [2]}
             [:rogue 1]
             42))))
  (testing "No vector, 0 index"
    (is (= {:rogue [42]}
           (update-hp-rolled
             {}
             [:rogue 0]
             42))))
  (testing "No vector, > 0 index"
    (is (= {:rogue [nil nil 42]}
           (update-hp-rolled
             {}
             [:rogue 2]
             42))))
  (testing "Nil map, > 0 index"
    (is (= {:rogue [nil nil 42]}
           (update-hp-rolled
             nil
             [:rogue 2]
             42)))))

