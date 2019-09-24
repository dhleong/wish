(ns wish.sheets.dnd5e.subs.combat-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sheets.dnd5e.subs.combat :refer [calculate-weapon]]))

(deftest calculate-weapon-test
  (let [proficient-cats #{:simple}
        proficient-kinds #{:spear}
        modifiers {:dex 3 :str 1}
        proficiency-bonus 2
        finesse-weapon-kinds #{:longsword}
        calc (fn [dmg-bonuses w & {:keys [effects-set]}]
               (calculate-weapon
                 proficient-cats
                 proficient-kinds
                 (or effects-set #{})
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

                          [:base-dice :alt-dice :to-hit]))))

    (testing "Computed bonus from effect"
      (let [buffs {:melee {:computed (fn [{:keys [effects modifiers]}]
                                        (when (:magic effects)
                                          (:dex modifiers)))}}
            weapon {:dice "1d8"}]

        ; with no effect, does the normal amount of damage
        (is (= "1d8 + 1"
               (:base-dice (calc buffs weapon))))

        ; with the effect, it computes the :dex bonus
        (is (= "1d8 + 4"
               (:base-dice (calc buffs weapon
                                 :effects-set #{:magic}))))))))
