(ns wish.util-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.util :refer [->map update-each-value]]))

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
