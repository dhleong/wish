(ns wish.sheets.dnd5e.subs.builder-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sheets.dnd5e.subs.builder :refer [available-classes]]))

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

