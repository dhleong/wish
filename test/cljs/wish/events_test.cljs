(ns wish.events-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.events :refer [apply-limited-use-trigger
                                 inventory-add
                                 inventory-subtract]]))

(deftest apply-limited-use-trigger-test
  (testing "Apply matching trigger"
    (is (= {:gas#uses 0
            :cargo#stored 0}
           (apply-limited-use-trigger
             {:gas#uses 2
              :cargo#stored 3}
             {:gas#uses {:restore-amount (constantly 2)
                         :restore-trigger :completed-job}
              :cargo#stored {:restore-amount (fn [{:keys [used]}]
                                               used)
                             :restore-trigger :completed-job}}
             :completed-job))))

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
                   :stacks? true
                   :default-quantity 20})]
      (is (= {:inventory {:arrows 20}}
             state))))

  (testing "Add custom stacked item"
    (let [state (inventory-add
                  {}
                  {:name "Fancy Arrows"
                   :stacks? true
                   :default-quantity 20})]
      (is (= 1 (count (:items state))))
      (let [inst-id (-> state :items keys first)]
        (is (= {:inventory {inst-id 20}
                :items {inst-id {:id inst-id
                                 :name "Fancy Arrows"
                                 :stacks? true
                                 :default-quantity 20}}}
               state)))))

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
               state)))))

  (testing "Add custom non-stacked item"
    (let [state (inventory-add
                  {}
                  {:name "Longbow"})]
      (is (= 1 (count (:items state))))
      (let [inst-id (-> state :items keys first)]
        (is (not (nil? inst-id)))
        (is (not= inst-id :longbow))
        (is (= {:inventory {inst-id 1}
                :items {inst-id {:id inst-id
                                 :name "Longbow"}}}
               state)))))

  (testing "Don't overwrite custom item id if it has one already"
    (let [state (inventory-add
                  {}
                  {:id :custom/longbow
                   :name "Longbow"})]
      (is (= 1 (count (:items state))))
      (let [inst-id (-> state :items keys first)]
        (is (not (nil? inst-id)))
        (is (not= inst-id :longbow))
        (is (= :custom/longbow inst-id))
        (is (= {:inventory {inst-id 1}
                :items {inst-id {:id inst-id
                                 :name "Longbow"}}}
               state))))))

(deftest inventory-subtract-test
  (testing "Subtract provided :stacks?"
    (let [state (inventory-subtract
                  {:inventory {:arrows 20}}
                  {:id :arrows})]
      (is (= {:inventory {:arrows 19}} state)))

    (let [state (inventory-subtract
                  {:inventory {:arrows 1}}
                  {:id :arrows})]
      (is (= {:inventory {}} (select-keys state [:inventory])))))

  (testing "Subtract provided non-:stacks?"
    (let [state (inventory-subtract
                  {:inventory {:longbow-inst-1 1}
                   :items {:longbow-inst-1 {:id :longbow}}}
                  {:id :longbow-inst-1})]
      (is (= {:inventory {}
              :items {}}
             state))))

  (testing "Subtract custom :stacks?"
    (let [state (inventory-subtract
                  {:inventory {:custom/arrows 1}
                   :items {:custom/arrows {:id :custom/arrows
                                           :stacks? true
                                           :name "Custom Arrows"}}}
                  {:id :custom/arrows
                   :stacks? true})]
      (is (= {:inventory {:custom/arrows 0}
              :items {:custom/arrows {:id :custom/arrows
                                      :stacks? true
                                      :name "Custom Arrows"}}}
             state)))))
