(ns wish.util.collections-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [wish.util.collections :refer [disj-by]]))

(deftest disj-by-test
  (testing "Remove first of multiple"
    (is (= [:serenity]
           (disj-by
             [:alliance :serenity]
             (partial = :alliance)))))

  (testing "Remove second of multiple"
    (is (= [:serenity]
           (disj-by
             [:serenity :alliance]
             (partial = :alliance)))))

  (testing "Do nothing when no match"
    (is (= [:mreynolds :itskaylee]
           (disj-by
             [:mreynolds :itskaylee]
             (partial = :alliance)))))
  )

