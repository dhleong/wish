(ns wish.sources.compiler.entity-mod-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sources.compiler.entity-mod :refer [extract-mod-and-key
                                                      apply-entity-mod]]))

(deftest extract-mod-and-key-test
  (testing "Concat"
    (is (= [concat :spells]
           (extract-mod-and-key :+spells)))
    (is (= [str :desc]
           (extract-mod-and-key :>>desc))))
  (testing "Merge"
    (is (= [merge :&levels]
           (extract-mod-and-key :&&levels)))))

(deftest apply-entity-mod-test
  (testing "Concat"
    (is (= {:spells [:a :b :c]}
           (apply-entity-mod
             {:spells [:a]}
             {:+spells [:b :c]}))))
  (testing "Features special case"
    (is (= {:features {:a true :b {:id :b}}}
           (apply-entity-mod
             {:features {:a true}}
             {:+features {:b {:id :b}}})))
    (is (= {:features {:a true :b {:id :b}}}
           (apply-entity-mod
             {:features {:a true}}
             {:+features [{:id :b}]})))
    (is (= {:features {:a true :b true}}
           (apply-entity-mod
             {:features {:a true}}
             {:+features [:b]})))))
