(ns wish.sheets.dnd5e.subs-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sheets.dnd5e.subs :as subs :refer [available-classes
                                                     unpack-eq-choices]]
            [wish.sheets.dnd5e.subs.test-util :refer [->ds]]))

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
                 `(declare-items
                    {}
                    ~dagger
                    ~lute
                    ~thieves-tools))

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

(deftest available-classes-test
  (testing "All classes available to be primary"
    (is (= [{:id :rogue}
            {:id :pilot}]
           (#'available-classes
             [{:id :rogue}
              {:id :pilot}]
             []  ; none selected
             nil ; none primary
             {})))))

