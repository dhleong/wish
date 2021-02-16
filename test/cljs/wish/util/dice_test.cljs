(ns wish.util.dice-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [wish.util.dice :refer [compute-average]]))

(deftest compute-average-test
  (testing "Average die roll with modifier"
    (is (= 1 (compute-average "1d4 - 1")))
    (is (= 2 (compute-average "1d4")))
    (is (= 3 (compute-average "1d4 + 1")))))

