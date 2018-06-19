(ns wish.sheets.dnd5e.subs-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sheets.dnd5e.subs :refer [level->proficiency-bonus
                                            spell-slots]]))

(def ^:private warlock
  {:attrs
   {:5e/spellcaster
    {:cantrips [1 2,
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
