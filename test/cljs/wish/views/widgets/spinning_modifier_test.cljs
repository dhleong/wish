(ns wish.views.widgets.spinning-modifier-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.views.widgets.spinning-modifier
             :refer [compute-rotation]]))

(deftest compute-rotation-test
  (testing "Rotate from 3 o'clock to 6 o'clock"
    (is (= 90
           (compute-rotation
             [0 0 10 10]

             ; touches:
             [10 5] [5 10]))))

  (testing "Rotate through 9 o'clock"
    (is (< 0
           (compute-rotation
             [0 0 10 10]

             [0 6] [0 4])))))

