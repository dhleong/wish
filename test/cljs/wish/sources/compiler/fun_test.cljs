(ns wish.sources.compiler.fun-test
  (:require [cljs.test :refer-macros [deftest testing is run-tests]]
            [wish.sources.compiler.fun :refer [let-args ->callable clean-form]]))

(deftest let-args-test
  (testing "No input"
    (is (= []
           (let-args '[]))))
  (testing "Single input"
    (is (= '[ship (:ship wish-fn-input)]
           (let-args '[ship]))))
  (testing "Multi input"
    (is (= '[ship (:ship wish-fn-input)
             cargo (:cargo wish-fn-input)]
           (let-args '[ship cargo])))))

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
                                              :dex 2}})))))

  (testing "Functions with (and)"
    (let [f (->callable '(fn [armor? shield?]
                           (when (and armor? shield?)
                             42)))]
      (is (= 42 (f {:armor? true :shield? true})))
      (is (= nil (f {:armor? true :shield? false})))
      (is (= nil (f {:armor? false :shield? true})))
      (is (= nil (f {:armor? false :shield? false})))))

  (testing "Functions with (or)"
    (let [f (->callable '(fn [armor? shield?]
                           (when (or armor? shield?)
                             42)))]
      (is (= 42 (f {:armor? true :shield? true})))
      (is (= 42 (f {:armor? true :shield? false})))
      (is (= 42 (f {:armor? false :shield? true})))
      (is (= nil (f {:armor? false :shield? false})))))

  (testing "Functions using keywords as functions"
    (let [f (->callable '(fn [options]
                           (:my/key options 9001)))]
      (is (= 42 (f {:options {:my/key 42}})))
      (is (= 9001 (f {:options {}})))))

  (testing "Compiled functions don't have access to js globals"
    (let [f (->callable '(fn [] js/window))]
      (is (nil? (f))))
    (let [f (->callable '(fn [] js/window.location))]
      (is (nil? (f))))))

(deftest ->compilable-test
  (testing "nop-out js/ symbols"
    (is (= nil
           (clean-form
             'js/window))))
  (testing "or -> cond"
    (is (= '(when (cond
                    dolls? dolls?
                    cows? cows?)
              :cargo)
           (clean-form
             '(when (or dolls? cows?)
                :cargo)))))

  (testing "and -> cond"
    (is (= '(when (wish.sources.compiler.fun/exported-not
                    (cond
                      (wish.sources.compiler.fun/exported-not dolls?) true
                      (wish.sources.compiler.fun/exported-not cows?) true))
              :cargo)
           (clean-form
             '(when (and dolls? cows?)
                :cargo)))))

  (testing "Rewrite using keywords as functions"
    (is (= '(wish.sources.compiler.fun/exported-get options :cargo)
           (clean-form
             '(:cargo options))))
    (is (= '(wish.sources.compiler.fun/exported-get options :cargo :-none)
           (clean-form
             '(:cargo options :-none)))))

  (testing "Rewrite using `(some)` with a set"
    (is (= '(wish.sources.compiler.fun/has? #{:geisha-dolls} coll)
           (clean-form
             '(some #{:geisha-dolls} coll))))

    ; but DON'T rewrite other uses of has
    (is (= '(wish.sources.compiler.fun/exported-some map? coll)
           (clean-form
             '(some map? coll))))))
