(ns wish.sheets.dnd5e.subs-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sheets.dnd5e.subs :refer [knowable-spell-counts-for
                                            level->proficiency-bonus
                                            available-slots
                                            spell-slots
                                            calculate-weapon
                                            unpack-eq-choices
                                            usable-slot-for]]
            [wish.sources.compiler :refer [compile-directives]]
            [wish.sources.core :as src :refer [->DataSource]]))

(defn- ->ds
  [& directives]
  (->DataSource :ds
                (compile-directives directives)))

(def ^:private warlock
  {:attrs
   {:5e/spellcaster
    {:cantrips [1 3,
                4 1,
                10 1]
     :ability :cha
     :spells :warlock/spells-list
     :extra-spells :warlock/extra-spells
     :prepares? false

     :slots-type :pact-magic
     :slots-label "Pact Magic"
     :slots {1 {1 1}, 2 {1 2}
             3 {2 2}, 4 {2 2}
             5 {3 2}, 6 {3 2}
             7 {4 2}, 8 {4 2}
             9 {5 2}, 10 {5 2}
             11 {5 3}, 12 {5 3}
             13 {5 3}, 14 {5 3}
             15 {5 3}, 16 {5 3}
             17 {5 4}, 18 {5 4}
             19 {5 4}, 20 {5 4}}
     :known [2 3 4 5 6 7 8 9 10 11 12 12 13 13 14 14 15 15 15 15]
     :multiclass-levels-mod 0
     :restore-trigger :short-rest
     }
    }})

(deftest level->proficiency-bonus-test
  (testing "Low levels"
    (is (= 2 (level->proficiency-bonus 1)))
    (is (= 2 (level->proficiency-bonus 2)))
    (is (= 2 (level->proficiency-bonus 4))))
  (testing "Higher levels"
    (is (= 3 (level->proficiency-bonus 5)))
    (is (= 3 (level->proficiency-bonus 8)))
    (is (= 4 (level->proficiency-bonus 9)))))

(defn- ->standard
  [slots]
  {:standard {:label "Spell Slots"
              :slots slots}})

(deftest knowable-spell-counts-for-test
  (testing "Table"
    (is (= {:spells 4
            :cantrips 3}
           (knowable-spell-counts-for
             (assoc warlock :level 3)
             {})))
    (is (= {:spells 11
            :cantrips 5}
           (knowable-spell-counts-for
             (assoc warlock :level 10)
             {})))
    (is (= 0
           (:spells
             (knowable-spell-counts-for
               {:level 2
                :attrs
                {:5e/spellcaster
                 {:known [0 0 3]}}}
               {})))))

  (testing "Standard (eg: cleric)"
    (is (= {:spells 10
            :cantrips 4}
           (knowable-spell-counts-for
             {:level 7
              :id :cleric
              :attrs
              {:5e/spellcaster
               {:slots :standard
                :cantrips [1 3,
                           4 1,
                           10 1]}}}
             {:cleric 3})))

    ; NOTE: :slots is omitted here; :standard is the default!
    (is (= {:spells 10
            :cantrips 4}
           (knowable-spell-counts-for
             {:level 7
              :id :cleric
              :attrs
              {:5e/spellcaster
               {:cantrips [1 3,
                           4 1,
                           10 1]}}}
             {:cleric 3})))))

(deftest spell-slots-test
  (testing "Single class, standard"
    (is (= (->standard {1 4, 2 3, 3 3, 4 1})
           (spell-slots [{:level 7}]))))
  (testing "Single class, half"
    (is (= (->standard {1 4, 2 3})
           (spell-slots [{:level 7
                          :attrs
                          {:5e/spellcaster
                           {:slots :standard/half}}}]))))
  (testing "Single class, Warlock-like"
    (is (= {:pact-magic
            {:label "Pact Magic"
             :slots {4 2}}}
           (spell-slots [(merge
                           warlock
                           {:level 7})]))))

  (testing "Multiclass, both standard"
    (is (= (->standard {1 4, 2 3, 3 3, 4 3, 5 2, 6 1, 7 1})
           (spell-slots [{:level 7}
                         {:level 7}]))))
  (testing "Multiclass, one half"
    (is (= (->standard {1 4, 2 3, 3 3, 4 3, 5 2})
           (spell-slots [{:level 7}
                         {:level 7
                          :attrs
                          {:5e/spellcaster
                           {:slots :standard/half
                            :multiclass-levels-mod 2}}}]))))
  (testing "Multiclass, one warlock-like"
    (is (= (assoc (->standard {1 4, 2 3, 3 3, 4 1})
                  :pact-magic
                  {:label "Pact Magic"
                   :slots {4 2}})
           (spell-slots [{:level 7}
                         (merge
                           warlock
                           {:level 7})])))))

(deftest calculate-weapon-test
  (let [proficient-cats #{:simple}
        proficient-kinds #{:spear}
        modifiers {:dex 3 :str 1}
        proficiency-bonus 2
        finesse-weapon-kinds #{:longsword}
        calc (fn [dmg-bonuses w]
               (calculate-weapon
                 proficient-cats
                 proficient-kinds
                 modifiers
                 proficiency-bonus
                 {} dmg-bonuses
                 finesse-weapon-kinds
                 w))]

    (testing "No proficiency bonus melee"
      (is (= {:base-dice "1d8 + 1"
              :to-hit 1}
             (select-keys (calc nil {:dice "1d8"})

               [:base-dice :to-hit]))))

    (testing "No proficiency bonus ranged"
      (is (= {:base-dice "1d8 + 3"
              :to-hit 3}
             (select-keys (calc nil {:dice "1d8"
                                     :ranged? true})

               [:base-dice :to-hit]))))

    (testing "Proficient melee"
      (is (= {:base-dice "1d8 + 1"
              :to-hit 3}
             (select-keys (calc nil {:dice "1d8"
                                     :kind :spear})

               [:base-dice :to-hit]))))

    (testing "Proficient ranged"
      (is (= {:base-dice "1d8 + 3"
              :to-hit 5}
             (select-keys (calc nil {:dice "1d8"
                                     :category :simple
                                     :ranged? true})

               [:base-dice :to-hit]))))

    (testing "Finesse melee"
      (is (= {:base-dice "1d8 + 3"
              :to-hit 3}
             (select-keys (calc nil {:dice "1d8"
                                     :finesse? true})

               [:base-dice :to-hit]))))

    (testing "Provided-Finesse melee"
      ; EG: monk weapon
      (is (= {:base-dice "1d8 + 3"
              :to-hit 3}
             (select-keys (calc nil {:dice "1d8"
                                     :kind :longsword})

                          [:base-dice :to-hit]))))

    (testing "Versatile weapon"
      (is (= {:base-dice "1d8 + 1"
              :alt-dice "1d10 + 1"
              :to-hit 1}
             (select-keys (calc nil {:dice "1d8"
                                     :versatile "1d10"})

                          [:base-dice :alt-dice :to-hit]))))

    (testing "Versatile + finesse weapon"
      (is (= {:base-dice "1d8 + 3"
              :alt-dice "1d10 + 3"
              :to-hit 3}
             (select-keys (calc nil {:dice "1d8"
                                     :kind :longsword
                                     :versatile "1d10"})

                          [:base-dice :alt-dice :to-hit]))))

    (testing "Versatile + finesse weapon + extra damage"
      (is (= {:base-dice "1d8 + 3 + 1d8"
              :alt-dice "1d10 + 3 + 1d8"
              :to-hit 3}
             (select-keys (calc {:melee {:paladin {:dice "1d8"
                                                   :type "radiant"}}}
                                {:dice "1d8"
                                 :kind :longsword
                                 :versatile "1d10"})

                          [:base-dice :alt-dice :to-hit]))))

    (testing "Fixed Damage bonus with filters"
      (is (= {:base-dice "1d8 + 3"
              :alt-dice "1d10 + 1"
              :to-hit 1}
             (select-keys (calc {:melee {:dueling {:+ 2
                                                   :when-two-handed? false
                                                   :when-versatile? false}}}
                                {:dice "1d8"
                                 :versatile "1d10"})

                          [:base-dice :alt-dice :to-hit]))))))

(deftest unpack-eq-choices-test
  (let [dagger {:id :dagger
                :type :weapon
                :kind :dagger
                :category :simple}
        lute {:id :lute
              :type :gear
              :kind :musical-instrument}
        thieves-tools {:id :thieves-tools
                       :type :other
                       :kind :tool}

        source (->ds
                 [:!declare-items
                  {}
                  dagger
                  lute
                  thieves-tools])

        packs {:explorers-pack {:backpack 1}}]

    (testing "Unpack 'or'"
      (is (= [:or [dagger lute]]
             (unpack-eq-choices
               source
               packs
               '(:dagger :lute)))))

    (testing "Unpack 'and'"
      (is (= [:and [dagger lute]]
             (unpack-eq-choices
               source
               packs
               '[:dagger :lute]))))

    (testing "Unpack (filter)"
      (is (= [:or [dagger]]
             (unpack-eq-choices
               source
               packs
               '{:type :weapon
                 :category :simple}))))

    (testing "Unpack (filter) with omitted key"
      (is (= [:or [dagger]]
             (unpack-eq-choices
               source
               packs
               '{:type :weapon
                 :ranged? false
                 :category :simple}))))

    (testing "Unpack (quantity)"
      (is (= [:count dagger 10]
             (unpack-eq-choices
               source
               packs
               '{:id :dagger
                 :count 10})))
      (is (= [:and [[:count dagger 10]]]
             (unpack-eq-choices
               source
               packs
               '[{:id :dagger
                  :count 10}]))))

    (testing "Nested choices"
      (is (= [:or [[:and [dagger thieves-tools]]
                   lute]]
             (unpack-eq-choices
               source
               packs
               '([:dagger :thieves-tools] :lute)))))))

(deftest available-slots-test
  (testing "Default slots"
    (is (= [{:kind :default
             :level 2
             :total 4
             :unused 2}]
           (available-slots
             {:default {:slots {1 4, 2 4}}}
             {:default {1 4, 2 2}}))))

  (testing "Mixed slots"
    (is (= [{:kind :default
             :level 1
             :total 4
             :unused 1}
            {:kind :pact-magic
             :level 1
             :total 2
             :unused 1}]
           (available-slots
             {:default {:slots {1 4}}
              :pact-magic {:slots {1 2}}}
             {:default {1 3}
              :pact-magic {1 1}})))))

(deftest usable-slot-for-test
  (testing "No downcasting"
    (is (nil? (usable-slot-for
                [{:kind :default
                  :level 1
                  :total 4
                  :unused 1}]
                {:spell-level 2}))))

  (testing "Same level"
    (is (= {:kind :default
            :level 1
            :total 4
            :unused 1}

           (usable-slot-for
             [{:kind :default
               :level 1
               :total 4
               :unused 1}]
             {:spell-level 1}))))

  (testing "Upcast"
    (is (= {:kind :default
            :level 2
            :total 4
            :unused 1}

           (usable-slot-for
             [{:kind :default
               :level 2
               :total 4
               :unused 1}]
             {:spell-level 1})))
    ))
