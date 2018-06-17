(ns wish.util-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [cljs.nodejs :as node]
            [wish.util :refer [->map]]))

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

