(ns wish.util.limited-use-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.util.limited-use :refer [restore-trigger-matches?]]))

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

