(ns wish.inventory-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.inventory :refer [stacks?]]))

(deftest stacks?-test
  (testing "Auto-stack by type"
    (is (true? (stacks?
                 {:type :ammunition})))
    (is (true? (stacks?
                 {:type :potion})))
    (is (false? (stacks?
                 {:type :weapon}))))
  (testing "Explicit :stacks?"
    (is (true? (stacks?
                 {:stacks? true})))
    (is (false? (stacks?
                 {:stacks? false})))))

