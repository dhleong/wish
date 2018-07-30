(ns wish.sources.compiler.feature-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sources.compiler.feature :refer [compile-max-options ]]))

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

  (testing "Compile features-based functional value"
    (let [f (compile-max-options
              '(fn [features]
                 (<= (count features) 2)))]
      (is (true? (f [])))
      (is (true? (f features-1)))
      (is (true? (f features-2)))
      (is (false? (f features-3)))))

  (testing "Compile numeric-functional value"
    (let [f (compile-max-options
              '(fn [] 2))]
      (is (true? (f [])))
      (is (true? (f features-1)))
      (is (true? (f features-2)))
      (is (false? (f features-3))))))

