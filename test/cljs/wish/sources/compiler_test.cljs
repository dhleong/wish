(ns wish.sources.compiler-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sources.compiler :refer [apply-levels apply-options compile-directives inflate]]
            [wish.sources.core :as src :refer [->DataSource]]))

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
                     :hit-dice/d10))))

  (testing "Apply directives of provided feature"
    ; for example, when the options are applied for
    ; a :background feature, that option's features get installed,
    ; but if they provide a feature (for example, proficiency with
    ; a skill) that feature's directives need to be applied (in this
    ; case, providing an attr.)
    (let [s (compile-directives
              [[:!provide-feature
                {:id :proficiency/stealth
                 :name "Stealth"
                 :! [[:!provide-attr :proficiency/stealth true]]}]

               [:!provide-feature
                {:id :background
                 :max-options 1}]

               [:!provide-options
                :background

                {:id :background/rogue
                 :primary-only? true
                 :name "Fake Rogue background"

                 :! [[:!provide-feature
                      :proficiency/stealth]]}]

               [:!declare-class
                {:id :fake-rogue
                 :features [:background]}]])
          ds (->DataSource :ds s)
          class-inst (src/find-class ds :fake-rogue)
          ch (inflate (assoc class-inst :primary? true)
                      ds
                      {:background [:background/rogue]})
          attrs (:attrs ch)]
      (is (identity class-inst))
      (is (= {:proficiency/stealth true} attrs))))

  (testing "Apply directives of provided feature option"
    ; for example, the options of the custom background feature
    (let [s (compile-directives
              [[:!add-to-list
                {:id :all-skill-proficiencies
                 :type :feature}

                {:id :proficiency/stealth
                 :name "Stealth"
                 :! [[:!provide-attr :proficiency/stealth true]]}]

               [:!provide-feature
                {:id :background
                 :max-options 1}]

               [:!provide-options
                :background

                {:id :background/custom
                 :primary-only? true
                 :name "Custom Background"
                 :! [[:!provide-feature

                      {:id :custom-bg/skill-proficiencies
                       :name "Skill proficiencies (Pick any 2)"
                       :implicit? true
                       :max-options 2
                       :values [:all-skill-proficiencies]}
                      ]]}]

               [:!declare-class
                {:id :fake-rogue
                 :features [:background]}]])
          ds (->DataSource :ds s)
          class-inst (src/find-class ds :fake-rogue)
          ch (inflate (assoc class-inst :primary? true)
                      ds
                      {:custom-bg/skill-proficiencies [:proficiency/stealth]
                       :background [:background/custom]})
          attrs (:attrs ch)]
      (is (identity class-inst))
      (is (= {:proficiency/stealth true} attrs)))))

(deftest provide-attr-test
  (testing "Provide in path"
    (let [s (compile-directives
              [[:!provide-attr
                [:5e/ac :monk/unarmored-defense]
                :value]])]
      (is (= {:attrs {:5e/ac {:monk/unarmored-defense :value}}}
             (select-keys s [:attrs]))))))

(deftest update-attr-test
  (testing "Update with math"
    (let [s (compile-directives
              '[[:!update-attr
                 [:buffs :dex]
                 +
                 1]])]
      (is (= {:attrs {:buffs {:dex 1}}}
             (select-keys s [:attrs]))))))


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
          applied (apply-options
                    ; NOTE: they must *have* the feature for the option
                    ; to get applied
                    {:features {:rogue/skill-proficiencies true}}
                    ds opts-map)]
      (is (= {:attrs {:proficiency/stealth true}}
             (select-keys applied [:attrs])))))

  (testing "Only apply :primary-only? options to :primary? class instance"
    (let [ds (->DataSource
               :source
               (compile-directives
                 [[:!provide-feature
                   {:id :proficiency/stealth
                    :name "Stealth"
                    :primary-only? true
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
          applied (apply-options
                    ; NOTE: they must *have* the feature for the option
                    ; to get applied
                    {:features {:rogue/skill-proficiencies true}}
                    ds opts-map)]
      (is (= {}
             (select-keys applied [:attrs])))))

  (testing "Apply options to a multi-instance feature"
    (let [ds (->DataSource
               :source
               (compile-directives
                 '[[:!provide-feature
                    {:id :ability/dex
                     :name "Stealth"
                     :! [[:!update-attr [:buffs :dex] + 1]]}]
                   [:!declare-class
                    {:id :cleric
                     :features
                     [{:id :ability-improvement
                       :name "Ability Score Improvement"
                       :max-options 2
                       :values [:ability/dex]}
                      ]}]]))
          opts-map {:ability-improvement [:ability/dex]
                    :ability-improvement#2 {:id :ability-improvement
                                            :value [:ability/dex]}}
          applied (apply-options
                    ; NOTE: they must *have* the feature for the option
                    ; to get applied
                    {:features {:ability-improvement 2}}
                    ds opts-map)]
      (is (= {:attrs {:buffs {:dex 2}}}
             (select-keys applied [:attrs]))))))

(deftest apply-levels-test
  (let [ds (->DataSource
             :source
             (compile-directives
               '[[:!provide-feature
                  {:id :proficiency/stealth
                   :name "Stealth"
                   :! [[:!provide-attr :proficiency/stealth true]]}
                  {:id :proficiency/insight
                   :name "Insight"
                   :! [[:!provide-attr :proficiency/insight true]]}
                  {:id :ability/dex
                   :name "Dexterity"
                   :! [[:!update-attr [:buffs :dex] + 1]]}]]))]

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
              (:attrs (apply-levels entity-base ds))))

        (is (= {:proficiency/stealth true}
               (:attrs (apply-levels level-2 ds))))
        (is (= {:proficiency/stealth true
                :proficiency/insight true}
               (:attrs (apply-levels level-3 ds))))))

    (testing "Support multiple instances of a feature"
      (let [class-def {:id :cleric
                       :&levels {2 {:+features
                                    [:ability/dex]}
                                 3 {:+features
                                    [:ability/dex]}
                                 4 {:+features
                                    [:ability/dex]}}}
            entity-base (assoc class-def :level 1)
            level-2 (assoc entity-base :level 2)
            level-3 (assoc entity-base :level 3)
            level-4 (assoc entity-base :level 4)]
        (is (= {:buffs {:dex 1}}
               (:attrs (apply-levels level-2 ds))))
        (is (= {:buffs {:dex 2}}
               (:attrs (apply-levels level-3 ds))))
        (let [applied-4 (apply-levels level-4 ds)]
          (is (= {:buffs {:dex 3}}
                 (:attrs applied-4)))
          (is (= 2 (-> applied-4 :features :ability/dex :wish/instances))))))

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
              (:attrs (apply-levels entity-base ds))))

        (is (= {:proficiency/stealth true}
               (:attrs (apply-levels level-2 ds))))
        (is (= {:proficiency/insight true}
               (:attrs (apply-levels level-3 ds))))))))
