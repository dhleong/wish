(ns wish.sources.compiler.lists-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sources.compiler.lists :refer [inflate-items
                                                 unpack-options-key]]))

(defn- ->just-ids
  [coll]
  (map #(select-keys % [:id]) coll))

(def state {:features {:crazy-ivan {:id :crazy-ivan}}
            :list-entities {:bobble-head-geisha {:id :bobble-head-geisha}}})

(deftest unpack-options-key-test
  (testing "unpack-options-key unpacks a keyword"
    (is (= :mreynolds
           (unpack-options-key
             :mreynolds>>options)))
    (is (= :mal/reynolds
           (unpack-options-key
             :mal/reynolds>>options))))
  (testing "unpack-options-key returns nil for non->>options keys"
    (is (nil? (unpack-options-key
                :mreynolds)))
    (is (nil? (unpack-options-key
                :mal/reynolds)))))

(deftest inflate-items-test
  (testing "Pass through maps"
    (is (= [{:id :mreynolds}]
           (->just-ids
                (inflate-items
                  state
                  nil
                  [{:id :mreynolds}])))))
  (testing "Inflate ids"
    (is (= [{:id :crazy-ivan}
            {:id :bobble-head-geisha}]
           (->just-ids
             (inflate-items
               state
               nil
               [:crazy-ivan :bobble-head-geisha])))))
  (testing "Inflate a list of ids"
    (is (= [{:id :crazy-ivan}
            {:id :bobble-head-geisha}]
           (->just-ids
             (inflate-items
               state
               nil
               [[:crazy-ivan :bobble-head-geisha]]))))))

