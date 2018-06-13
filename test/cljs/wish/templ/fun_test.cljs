(ns wish.templ.fun-test
  (:require [cljs.test :refer-macros [deftest testing is run-tests]]
            [cljs.nodejs :as node]
            [wish.templ.fun :refer [->callable]]))

(deftest ->callable-test
  (testing "Constant values"
    (let [f (->callable '2)]
      (is (= 2 (f)))
      (is (= 2 (f))))
    (let [f (->callable '"serenity")]
      (is (= "serenity" (f)))))

  (testing "Simple functions"
    (let [f (->callable '(fn []
                           42))]
      (is (= 42 (f)))
      (is (= 42 (f)))))

  (testing "Functions with params"
    (let [f (->callable '(fn [value]
                           (* value 42)))]
      (is (= 84 (f {:value 2})))))

  (testing "Functions with rounding"
    (let [f (->callable '(fn [value]
                           (ceil (/ value 3))))]
      (is (= 3 (f {:value 8}))))
    (let [f (->callable '(fn [value]
                           (floor (/ value 3))))]
      (is (= 2 (f {:value 8}))))))

