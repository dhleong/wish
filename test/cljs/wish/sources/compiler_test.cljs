(ns wish.sources.compiler-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [cljs.nodejs :as node]
            [wish.sources.compiler :refer [compile-max-options compile-directives]]))

(def character-state
  {:level 42})

(def features-1
  {:features [{:level 1}]})

(def features-2
  {:features [{:level 1} {:level 2}]})

(def features-3
  {:features [{:level 1} {:level 2} {:level 3}]})

(deftest features-test
  (testing "Compile constant value"
    (let [f (compile-max-options 1)]
      (is (true? (f [])))
      (is (true? (f features-1)))
      (is (false? (f features-2)))
      (is (false? (f features-3)))))

  (testing "Compile functional value"
    (let [f (compile-max-options
              '(fn [features]
                 (<= (count features) 2)))]
      (is (true? (f [])))
      (is (true? (f features-1)))
      (is (true? (f features-2)))
      (is (false? (f features-3)))))

  ;; TODO filter support
  #_(testing "Compile filter-list value"
    (is (= 42 ((compile-max-options
                 [:total 4])
               character-state)))))

(deftest provide-feature-test
  (testing "Simple provide"
    (let [s (compile-directives
              [[:!provide-feature
                {:id :hit-dice/d10
                 :name "Hit Dice: D10"}]])]
      (is (contains? s :features))
      (is (contains? (:features s)
                     :hit-dice/d10)))))
