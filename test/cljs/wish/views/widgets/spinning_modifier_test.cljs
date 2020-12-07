(ns wish.views.widgets.spinning-modifier-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.views.widgets.spinning-modifier
             :refer [compute-rotation]]))

(deftest compute-rotation-test
  (testing "Rotate"
    (is (= 90
           (compute-rotation
             [0 0 10 10]

             ; 3 o'clock to 6 o'clock
             [10 5] [5 10])))))

