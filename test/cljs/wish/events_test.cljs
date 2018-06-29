(ns wish.events-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.events :refer [apply-limited-use-trigger
                                 restore-trigger-matches?
                                 inventory-add]]))

(deftest restore-trigger-matches
  (testing "Keyword case is simple"
    (is (true? (restore-trigger-matches?
                 :required
                 :required)))
    (is (false? (restore-trigger-matches?
                 :required
                 :other))))
  (testing "Sets are handled"
    (is (true? (restore-trigger-matches?
                 #{:required}
                 :required)))
    (is (false? (restore-trigger-matches?
                 #{:required}
                 :other))))
  (testing "Other collections work"
    (is (true? (restore-trigger-matches?
                 [:required]
                 :required)))
    (is (false? (restore-trigger-matches?
                 [:required]
                 :other)))))

(deftest apply-limited-use-trigger-test
  (testing "Do nothing for mismatched triggers"
    (is (= {:gas#uses 2}
           (apply-limited-use-trigger
             {:gas#uses 2}
             {:gas#uses {:restore-amount (constantly 2)
                         :restore-trigger :completed-job}}
             :picked-up-cargo)))))

(deftest inventory-add-test
  (testing "Add stacked item"
    (let [state (inventory-add
                  {}
                  {:id :arrows
                   :attrs {:default-quantity 20}})]
      (is (= {:inventory {:arrows 20}}))))

  (testing "Add non-stacked item instance"
    (let [state (inventory-add
                  {}
                  {:id :longbow})]
      (is (= 1 (count (:items state))))
      (let [inst-id (-> state :items keys first)]
        (is (not (nil? inst-id)))
        (is (not= inst-id :longbow))
        (is (= {:inventory {inst-id 1}
                :items {inst-id {:id :longbow}}}
               state))))))
