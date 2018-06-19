(ns wish.sources.compiler-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sources.compiler :refer [apply-options compile-directives]]
            [wish.sources.core :refer [->DataSource]]))

(def character-state
  {:level 42})

(deftest provide-feature-test
  (testing "Simple provide"
    (let [s (compile-directives
              [[:!provide-feature
                {:id :hit-dice/d10
                 :name "Hit Dice: D10"}]])]
      (is (contains? s :features))
      (is (contains? (:features s)
                     :hit-dice/d10)))))

(deftest class-test
  (testing "Inflate features by id"
    (let [s (compile-directives
              [[:!declare-class
                {:id :classy
                 :features [:hit-dice/d10]}]
               [:!provide-feature
                {:id :hit-dice/d10
                 :name "Hit Dice: D10"}]])]
      (is (contains? s :classes))
      (is (contains? (:classes s)
                     :classy))
      (is (= :hit-dice/d10
             (-> s :classes :classy :features :hit-dice/d10 :id)))))
  (testing "Apply feature directives when installed"
    (let [s (compile-directives
              [[:!declare-class
                {:id :classy
                 :features [:hit-dice/d10]}]
               [:!provide-feature
                {:id :hit-dice/d10
                 :! [[:!provide-attr
                      :5e/hit-dice 10]]}]])]
      (is (contains? s :classes))
      (is (contains? (:classes s)
                     :classy))
      (is (= 10
             (-> s :classes :classy :attrs :5e/hit-dice))))))

(deftest options-test
  (testing "Options provided before the feature"
    (let [s (compile-directives
              [[:!provide-options
                :feature
                {:id :feature/opt1}
                {:id :feature/opt2}]

               [:!provide-feature
                {:id :feature
                 :max-options 1}]])
          f (get-in s [:features :feature])]
      (is (= [{:id :feature/opt1}
              {:id :feature/opt2}]
             (:values f))))))

(deftest apply-options-test
  (testing "Apply options to class instance"
    (let [ds (->DataSource
               :source
               (compile-directives
                 [[:!provide-feature
                   {:id :proficiency/stealth
                    :name "Stealth"
                    :! [[:!provide-attr :proficiency/stealth true]]}]
                  [:!declare-class
                   {:id :cleric
                    :features
                    [{:id :rogue/skill-proficiencies
                      :name "Proficiencies"
                      :max-options 2
                      :values [:proficiency/stealth]}
                     ]}]]))
          opts-map {:rogue/skill-proficiencies [:proficiency/stealth]}
          applied (apply-options {} ds opts-map)]
      (is (= {:attrs {:proficiency/stealth true}}
             applied)))))
