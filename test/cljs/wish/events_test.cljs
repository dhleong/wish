(ns wish.events-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.events :refer [apply-limited-use-trigger
                                 restore-trigger-matches?]]))

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

