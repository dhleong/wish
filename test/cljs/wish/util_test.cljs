(ns wish.util-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.util :refer [->map padded-compare update-each-value]]))

(deftest ->map-test
  (testing "Vector ->map"
    (is (= {:mreynolds {:id :mreynolds}
            :zoe {:id :zoe}}
           (->map [{:id :mreynolds}
                   {:id :zoe}]))))
  (testing "Lazy seq ->map"
    (is (= {:mreynolds {:id :mreynolds}
            :zoe {:id :zoe}}
           (->map (concat
                    [{:id :mreynolds}]
                    [{:id :zoe}]))))))

(deftest update-each-value-test
  (testing "Empty cases"
    (is (= nil (update-each-value nil inc)))
    (is (= {} (update-each-value {} inc))))
  (testing "Update each value"
    (is (= {:one 2
            :three 4}
           (update-each-value
             {:one 1 :three 3}
             inc)))))

(deftest padded-compare-test
  (testing "Compare same length"
    (is (= 0 (padded-compare [1] [1])))
    (is (= 0 (padded-compare [1 1] [1 1])))
    (is (= 0 (padded-compare [1 1 1] [1 1 1]))))
  (testing "Compare (count a) < (count b) "
    (is (= 0 (padded-compare [1] [1 0])))
    (is (< (padded-compare [1] [1 1])
           0)))
  (testing "Compare (count a) > (count b) "
    (is (= 0 (padded-compare [1 0] [1])))
    (is (> (padded-compare [1 1] [1])
           0))))
