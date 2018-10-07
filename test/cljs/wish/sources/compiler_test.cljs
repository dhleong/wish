(ns wish.sources.compiler-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sources.compiler :as c :refer [apply-levels apply-options compile-directives inflate]]
            [wish.sources.compiler.limited-use :as lu]
            [wish.sources.core :as src :refer [->DataSource]]))

(def character-state
  {:level 42})

(def apply-feature-levels #(apply-levels %1 %2 c/find-feature-scaling))
(def apply-limited-levels #(apply-levels %1 %2 c/find-limited-use-scaling))

(deftest apply-mod-in-test
  (testing "Simple replacement"
    (is (= {:limited-uses
            {:hard-burn
             {:id :hard-burn
              :restore-trigger :short-rest}}}
           (-> (c/apply-mod-in
                 {:limited-uses
                  {:hard-burn
                   {:id :hard-burn
                    :restore-trigger :long-rest}}}
                 nil
                 {:restore-trigger :short-rest}
                 [:limited-uses :hard-burn])
               (select-keys [:limited-uses])))))

  (testing "Level-scale new features"
    (is (= {:id :hard-burn
            :desc "Burn baby burn"}
           (-> (c/apply-mod-in
                 {:level 3}
                 nil
                 {:+features
                  [{:id :hard-burn
                    :desc "Burn"
                    :levels {3 {:>>desc " baby burn"}}}]}
                 nil)
               :features
               :hard-burn
               (select-keys [:id :desc]))))))

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
      (is (= [{:id :feature/opt1 :available? nil}
              {:id :feature/opt2 :available? nil}]
             (:values f))))))

(deftest apply-options-test
  (let [simple-ds (->DataSource
                    :source
                    (compile-directives
                      [[:!provide-feature
                        {:id :proficiency/stealth
                         :name "Stealth"
                         :primary-only? true
                         :! [[:!provide-attr :proficiency/stealth true]]

                         ; this is bogus, but useful for testing
                         :&levels {3 {:+! [[:!provide-attr
                                            :expertise/stealth
                                            true]]}}}]
                       [:!declare-class
                        {:id :cleric
                         :features
                         [{:id :rogue/skill-proficiencies
                           :name "Proficiencies"
                           :max-options 2
                           :values [:proficiency/stealth]}
                          ]}]]))]

    (testing "Apply options to class instance"
      (let [ds simple-ds
            opts-map {:rogue/skill-proficiencies [:proficiency/stealth]}
            applied (apply-options
                      ; NOTE: they must *have* the feature for the option
                      ; to get applied
                      {:features {:rogue/skill-proficiencies true}
                       :primary? true}
                      ds opts-map)]
        (is (= {:attrs {:proficiency/stealth true}}
               (select-keys applied [:attrs])))))

    (testing "Only apply :primary-only? options to :primary? class instance"
      (let [ds simple-ds
            opts-map {:rogue/skill-proficiencies [:proficiency/stealth]}
            applied (apply-options
                      ; NOTE: they must *have* the feature for the option
                      ; to get applied
                      {:features {:rogue/skill-proficiencies true}}
                      ds opts-map)]
        (is (= {}
               (select-keys applied [:attrs])))))

    (testing "Level-scale options on class instance"
      (let [ds simple-ds
            opts-map {:rogue/skill-proficiencies [:proficiency/stealth]}
            applied (apply-options
                      ; NOTE: see above note
                      {:features {:rogue/skill-proficiencies true}
                       :primary? true
                       :level 3}
                      ds opts-map)]
        (is (= {:attrs {:proficiency/stealth true
                        :expertise/stealth true}}
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
               (select-keys applied [:attrs])))))))

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
              (:attrs (apply-feature-levels entity-base ds))))

        (is (= {:proficiency/stealth true}
               (:attrs (apply-feature-levels level-2 ds))))
        (is (= {:proficiency/stealth true
                :proficiency/insight true}
               (:attrs (apply-feature-levels level-3 ds))))))

    (testing "Don't duplicate when combining feature levels"
      (let [class-def {:id :cleric
                       :features
                       {:test-feature
                        ; REMINDER: though it's a list in the edn
                        ; file for simplicity, it gets inflated to
                        ; a map
                        {:id :test-feature
                         :! [[:!add-to-list
                              :feature-list
                              {:id :1}]]
                         :&levels {2 {:+! [[:!add-to-list
                                            :feature-list
                                            {:id :2}]]}
                                   3 {:+! [[:!add-to-list
                                            :feature-list
                                            {:id :3}]]}}} }}
            entity-base (assoc class-def :level 1)
            level-2 (assoc entity-base :level 2)
            level-3 (assoc entity-base :level 3) ]
        (is (= [:1 :2]
               (->> (apply-feature-levels level-2 ds)
                    :lists :feature-list
                    (map :id))))
        (is (= [:1 :2 :3]
               (->> (apply-feature-levels level-3 ds)
                    :lists :feature-list
                    (map :id))))))

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
               (:attrs (apply-feature-levels level-2 ds))))
        (is (= {:buffs {:dex 2}}
               (:attrs (apply-feature-levels level-3 ds))))
        (let [applied-4 (apply-feature-levels level-4 ds)]
          (is (= {:buffs {:dex 3}}
                 (:attrs applied-4)))
          (is (= 3 (-> applied-4 :features :ability/dex :wish/instances))))))

    (testing "Support providing multiple instances of a feature"
      ; NOTE: as noted elsewhere, once compiled the :features for a class
      ; becomes a map
      (let [class-def {:id :cleric
                       :features
                       {:dex-options
                        {:id :dex-options
                         :max-options 2
                         :values [{:id :dex1
                                   :! [[:!provide-feature :ability/dex]]}
                                  {:id :dex2
                                   :! [[:!provide-feature :ability/dex]]}
                                  {:id :dex3
                                   :! [[:!provide-feature :ability/dex]]}]} }}
            opts1 {:dex-options [:dex1]}
            opts2 {:dex-options [:dex1 :dex2]}
            opts3 {:dex-options [:dex1 :dex2 :dex3]}
            ]
        (is (= {:buffs {:dex 1}}
               (:attrs (inflate class-def ds opts1))))
        (is (= {:buffs {:dex 2}}
               (:attrs (inflate class-def ds opts2))))
        (let [applied-3 (inflate class-def ds opts3)]
          (is (= {:buffs {:dex 3}}
                 (:attrs applied-3)))
          (is (= 3 (-> applied-3 :features :ability/dex :wish/instances))))))

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
              (:attrs (apply-feature-levels entity-base ds))))

        (is (= {:proficiency/stealth true}
               (:attrs (apply-feature-levels level-2 ds))))
        (is (= {:proficiency/insight true}
               (:attrs (apply-feature-levels level-3 ds))))))

    (testing "Only scale once"
      (let [class-def {:id :sorcerer
                       :features
                       {:rogue/sneak-attack
                        {:id :rogue/sneak-attack
                         :desc ""
                         :levels {1 {:>>desc "Level 1"}
                                  2 {:>>desc "Level 2"}
                                  3 {:>>desc "Level 3"}
                                  4 {:>>desc "Level 4"}}} }}
            entity-base (assoc class-def :level 1)
            level-4 (assoc entity-base :level 4)]
        (is (= "Level 1"
               (-> (inflate entity-base ds {})
                   :features
                   :rogue/sneak-attack
                   :desc)))
        (is (= "Level 4"
               (-> (inflate level-4 ds {})
                   :features
                   :rogue/sneak-attack
                   :desc)))))

    (testing "Apply level scaling to limited-use"
      (let [class-def {:id :sorcerer
                       :&levels
                       {2 {:+features
                           [{:id :spell-points
                             :! [[:!add-limited-use
                                  {:id :spell-points
                                   :restore-trigger :long-rest
                                   :levels {20 {:restore-trigger :short-rest}}}]]}]}}}
            entity-base (assoc class-def :level 2)
            level-20 (assoc entity-base :level 20)]
        (is (= :long-rest
               (-> (inflate entity-base ds {})
                   :limited-uses
                   :spell-points
                   :restore-trigger)))
        (is (= :short-rest
               (-> (inflate level-20 ds {})
                   :limited-uses
                   :spell-points
                   :restore-trigger)))))))

; as features get compiled and installed into entities,
; we add :wish/sort to them to ensure that, when iterating
; over all the features installed into an entity we can do
; so in the order the data-source author intended.
; This includes sorting features provided by a feature to
; come immediately after the feature they were provided by,
; and sorting features added by level-scaling in ascending
; order by the level they were added
(deftest sort-key-test
  (testing "Basic :features"
    (let [class-def (->> (compile-directives
                           [[:!declare-class
                             {:id :cleric
                              :features
                              [{:id :first}
                               {:id :second}]}]])
                         :classes
                         :cleric)]
      (is (= [0 0] (-> class-def :features :first :wish/sort)))
      (is (= [0 1] (-> class-def :features :second :wish/sort)))))

  (testing ":+features"
    (let [class-def (-> (compile-directives
                           [[:!declare-class
                             {:id :cleric
                              :features
                              [{:id :first}]
                              :&levels
                              {2 {:+features [{:id :second}]}}}]])
                         :classes
                         :cleric
                         (assoc :level 3) ; ensure sort is based on level *added*,
                                          ; not level *when added*
                         (inflate nil {}))]
      (is (= [0 0] (-> class-def :features :first :wish/sort)))
      (is (= [2 0] (-> class-def :features :second :wish/sort)))))

  (testing "Basic :!provide-feature"
    (let [class-def (-> (compile-directives
                          [[:!declare-class
                            {:id :cleric
                             :features
                             [{:id :base
                               :! [[:!provide-feature
                                    {:id :provided}]]}]}]])
                        :classes
                        :cleric
                        (inflate (->DataSource :id {})
                                 {}))]
      (is (= [0 0] (-> class-def :features :base :wish/sort)))
      (is (= [0 0 1] (-> class-def :features :provided :wish/sort)))))

  (testing ":!provide-feature as option"
    (let [class-def (-> (compile-directives
                          [[:!declare-class
                            {:id :cleric
                             :features
                             [{:id :base
                               :max-options 1
                               :values
                               [{:id :provider
                                 :! [[:!provide-feature
                                      {:id :provided}]]}]}]}]])
                        :classes
                        :cleric
                        (inflate (->DataSource :id {})
                                 {:base [:provider]}))]
      (is (= [0 0] (-> class-def :features :base :wish/sort)))
      (is (= [0 0 1] (-> class-def :features :provided :wish/sort)))))

  (testing ":!provide-feature as instanced option"
    (let [state (compile-directives
                  [[:!provide-feature
                    {:id :child
                     :instanced? true
                     :max-options 1}
                    {:id :instanced
                     :instanced? true
                     :max-options 1
                     :values
                     [{:id :provide/child
                       :! [[:!provide-feature :child]]}]}]
                   [:!declare-class
                    {:id :cleric
                     :&levels
                     {1 {:+features [:instanced]}
                      2 {:+features [:instanced]}
                      3 {:+features [:instanced]}
                      }}]])
          class-def (-> state
                        :classes
                        :cleric
                        (assoc :level 4)
                        (inflate (->DataSource :id state)
                                 {:instanced#rogue#0
                                  {:id :instanced
                                   :value [:provide/child]}
                                  :instanced#rogue#1
                                  {:id :instanced
                                   :value [:provide/child]}
                                  :instanced#rogue#2
                                  {:id :instanced
                                   :value [:provide/child]}}))]
      (is (= '([3 0] [2 0] [1 0])
             (-> class-def :features :instanced :wish/sorts)))
      (is (= '([3 0 1] [2 0 1] [1 0 1])
             (-> class-def :features :child :wish/sorts))))))
