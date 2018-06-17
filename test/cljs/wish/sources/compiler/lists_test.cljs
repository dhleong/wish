(ns wish.sources.compiler.lists-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [cljs.nodejs :as node]
            [wish.sources.compiler.lists :refer [inflate-items]]))

(def state {:features {:crazy-ivan {:id :crazy-ivan}}
            :list-entities {:bobble-head-geisha {:id :bobble-head-geisha}}})

(deftest inflate-items-test
  (testing "Pass through maps"
    (is (= [{:id :mreynolds}]
           (inflate-items
             state
             [{:id :mreynolds}]))))
  (testing "Inflate ids"
    (is (= [{:id :crazy-ivan}
            {:id :bobble-head-geisha}]
           (inflate-items
             state
             [:crazy-ivan :bobble-head-geisha]))))
  (testing "Inflate a list of ids"
    (is (= [{:id :crazy-ivan}
            {:id :bobble-head-geisha}]
           (inflate-items
             state
             [[:crazy-ivan :bobble-head-geisha]])))))

