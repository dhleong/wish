(ns wish.sheets.dnd5e.overlays.custom-item-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sheets.dnd5e.overlays.custom-item
             :refer [install-limited-use
                     item->form-state]]))

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
            :consumes :serenity#uses
            :! '(on-state
                  (add-limited-use
                    {:id :serenity#uses
                     :name "Crazy Ivan"
                     :uses 2
                     :restore-trigger :short-rest})
                  (provide-attr
                    [:bonus :serenity]
                    true))}

           (install-limited-use {:id :serenity
                                 :limited-use? true
                                 :attunes? true
                                 :limited-use
                                 {:name "Crazy Ivan"
                                  :restore-trigger :short-rest
                                  :action-type :bonus
                                  :uses 2}}))))

  (testing "Install with nothing provided"
    (is (= {:id :serenity
            :name "Serenity"
            :! '(on-state
                  (add-limited-use
                    {:id :serenity#uses
                     :name "Serenity"
                     :uses 1
                     :restore-trigger :long-rest}))}

           (install-limited-use {:id :serenity
                                 :name "Serenity"
                                 :limited-use? true})))))

(deftest item->form-state-test
  (testing "Normal pass-through"
    (is (= {:id :serenity
            :type :other
            :name "Serenity"}
           (item->form-state
             {:id :serenity
              :type :other
              :name "Serenity"}))))

  (testing "Limited use w/action"
    (is (= {:id :serenity
            :type :gear
            :limited-use? true
            :limited-use
            {:action-type :bonus
             :uses 1
             :restore-trigger :short-rest
             :name "Crazy Ivan"}}

           (item->form-state
             '{:id :serenity
               :type :gear
               :consumes :serenity#uses
               :limited-uses {:serenity {:id :serenity#uses
                                         :name "Crazy Ivan"
                                         :uses 1
                                         :restore-trigger :short-rest}}
               :attrs {:bonus {:serenity true}}
               :!-raw (on-state
                        (add-limited-use
                          {:id :serenity#uses
                           :name "Crazy Ivan"
                           :uses 1
                           :restore-trigger :short-rest})
                        (provide-attr
                          [:bonus :serenity]
                          true))})))))
