(ns wish.sources.compiler-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sources.compiler :refer [apply-levels apply-options compile-directives]]
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

(deftest apply-levels-test
  (let [ds (->DataSource
             :source
             (compile-directives
               [[:!provide-feature
                 {:id :proficiency/stealth
                  :name "Stealth"
                  :! [[:!provide-attr :proficiency/stealth true]]}
                 {:id :proficiency/insight
                  :name "Insight"
                  :! [[:!provide-attr :proficiency/insight true]]}]

                [:!declare-class class-def]]))
        opts-map {:rogue/skill-proficiencies [:proficiency/stealth]}]

    (testing "Combine levels"
      (let [class-def {:id :cleric
                       :&levels {2 {:+features
                                    [:proficiency/stealth]}
                                 3 {:+features
                                    [:proficiency/insight]}}}
            entity-base (assoc class-def :level 1)
            level-2 (assoc entity-base :level 2)
            level-3 (assoc entity-base :level 3) ]
        (is (nil?
              (:attrs (apply-levels entity-base ds opts-map))))

        (is (= {:proficiency/stealth true}
               (:attrs (apply-levels level-2 ds opts-map))))
        (is (= {:proficiency/stealth true
                :proficiency/insight true}
               (:attrs (apply-levels level-3 ds opts-map))))))

    (testing "Replace levels"
      ; This is a contrived example, but...
      (let [class-def {:id :cleric
                       :levels {2 {:+features
                                   [:proficiency/stealth]}
                                3 {:+features
                                   [:proficiency/insight]}}}
            entity-base (assoc class-def :level 1)
            level-2 (assoc entity-base :level 2)
            level-3 (assoc entity-base :level 3) ]
        (is (nil?
              (:attrs (apply-levels entity-base ds opts-map))))

        (is (= {:proficiency/stealth true}
               (:attrs (apply-levels level-2 ds opts-map))))
        (is (= {:proficiency/insight true}
               (:attrs (apply-levels level-3 ds opts-map))))))))
