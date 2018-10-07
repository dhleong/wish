(ns wish.sources.compiler.util-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sources.compiler.util :refer [combine-sorts]]))

(defn do-combine-sorts
  [a b]
  (:wish/sorts (combine-sorts a b)))

(deftest combine-sorts-test
  (testing "No existing sorts, one sort"
    (is (= '([0 1])
           (do-combine-sorts
             {:wish/sort [0 1]}
             {})))
    (is (= '([0 1])
           (do-combine-sorts
             {}
             {:wish/sort [0 1]}))))
  (testing "No existing sorts, both sort"
    (is (= '([0 2] [0 1])
           (do-combine-sorts
             {:wish/sort [0 1]}
             {:wish/sort [0 2]})))
    (is (= '([0 2] [0 1])
           (do-combine-sorts
             {:wish/sort [0 2]}
             {:wish/sort [0 1]}))))
  (testing "Existing sorts"
    (is (= '([0 2] [0 1])
           (do-combine-sorts
             {:wish/sorts '([0 1])}
             {:wish/sort [0 2]})))))

