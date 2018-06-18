(ns wish.templ.fun-test
  (:require [cljs.test :refer-macros [deftest testing is run-tests]]
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

  (testing "Idempotent"
    (let [f (->callable
              (->callable '(fn []
                             42)))]
      (is (= 42 (f)))
      (is (identical? f (->callable f)))))

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
      (is (= 2 (f {:value 8})))))

  (testing "Functions with conditions"
    ; from Monk's unarmed strike
    (let [f (->callable '(fn [level modifiers]
                           (let [m (max (:str modifiers)
                                        (:dex modifiers))]
                             (str "1d"
                                  (cond
                                    (<= level 4) "4"
                                    (<= level 10) "6"
                                    (<= level 16) "8"
                                    (<= level 20) "10")
                                  "+" m))))]
      (is (= "1d4+2" (f {:level 4 :modifiers {:str 1
                                              :dex 2}})))
      (is (= "1d6+4" (f {:level 8 :modifiers {:str 4
                                              :dex 2}}))))))

