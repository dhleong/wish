(ns wish.sources.compiler.feature-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sources.compiler.feature :refer [compile-feature
                                                   compile-max-options]]))

(def features-1
  {:features [{:level 1}]})

(def features-2
  {:features [{:level 1} {:level 2}]})

(def features-3
  {:features [{:level 1} {:level 2} {:level 3}]})

(deftest compile-max-options-test
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

(deftest compile-feature-test
  (testing "Insert :!provide-attr from :availability-attr"
    (let [f (compile-feature {:availability-attr :single-id})]
      (is (= [[:!provide-attr :single-id true]]
             (:! f))))
    (let [f (compile-feature {:availability-attr [:id :path]})]
      (is (= [[:!provide-attr [:id :path] true]]
             (:! f)))))

  (testing "Insert :available? from :availability-attr"
    (let [f (compile-feature {:availability-attr :single-id})]
      (is (true? ((:available? f) {:attrs {}})))
      (is (false? ((:available? f) {:attrs {:single-id true}}))))
    (let [f (compile-feature {:availability-attr [:id :path]})]
      (is (true? ((:available? f) {:attrs {}})))
      (is (true? ((:available? f) {:attrs {:id true}})))
      (is (false? ((:available? f) {:attrs {:id {:path true}}})))))

  (testing "Combine :available? with :availability-attr"
    (let [f (compile-feature '{:availability-attr :single-id
                               :available? (fn [available?]
                                             (str "!" available?))})]
      (is (= "!true"
             ((:available? f) {:attrs {}})))
      (is (= "!false"
             ((:available? f) {:attrs {:single-id true}}))))
    (let [f (compile-feature '{:availability-attr [:id :path]
                               :available? (fn [available?]
                                             (str "!" available?))})]
      (is (= "!true"
             ((:available? f) {:attrs {}})))
      (is (= "!true"
             ((:available? f) {:attrs {:id true}})))
      (is (= "!false"
             ((:available? f) {:attrs {:id {:path true}}})))))

  (testing "Neither :available? nor :availability-attr"
    (let [f (compile-feature '{})]
      (is (nil? (:available? f))))))
