(ns wish.inventory-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [cljs.reader :as edn]
            [wish.inventory :as inv :refer [stacks?]]))

(deftest custom-id-test
  (testing "Custom-ids can be read back from edn"
    (let [n "500GP of Gems"
          id (inv/custom-id n)]
      (= id
         (edn/read-string
           (str id))))))

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

