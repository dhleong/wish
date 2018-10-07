(ns wish.sheets.dnd5e.overlays.custom-item-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sheets.dnd5e.overlays.custom-item :refer [install-limited-use]]))

(deftest install-limited-use-test
  (testing "Ignore when-not limited-use?"
    (is (= {}
           (install-limited-use {})))
    (is (= {}
           (install-limited-use {:limited-use? false
                                 :attunes? true
                                 :limited-use {:name "Name"}}))))

  (testing "Install with everything provided"
    (is (= {:id :serenity
            :attunes? true
            :! [[:!add-limited-use
                 {:id :serenity
                  :name "Crazy Ivan"
                  :uses 2
                  :restore-trigger :short-rest}]]}

           (install-limited-use {:id :serenity
                                 :limited-use? true
                                 :attunes? true
                                 :limited-use
                                 {:name "Crazy Ivan"
                                  :restore-trigger :short-rest
                                  :uses 2}}))))

  (testing "Install with nothing provided"
    (is (= {:id :serenity
            :name "Serenity"
            :! [[:!add-limited-use
                 {:id :serenity
                  :name "Serenity"
                  :uses 1
                  :restore-trigger :long-rest}]]}

           (install-limited-use {:id :serenity
                                 :name "Serenity"
                                 :limited-use? true})))))


