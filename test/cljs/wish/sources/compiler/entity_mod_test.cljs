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
    (is (= [apply-entity-mod :&levels]
           (extract-mod-and-key :&&levels))))
  (testing "Merge with namespace"
    (is (= [apply-entity-mod :5e/levels]
           (extract-mod-and-key :5e/&levels)))))

(deftest apply-entity-mod-test
  (testing "Concat"
    (is (= {:spells [:a :b :c]}
           (apply-entity-mod
             {:spells [:a]}
             {:+spells [:b :c]}))))

  (testing "Recursive merge"
    (is (= {:attrs {:5e/ability-score-increase {:dex 2
                                                :int 1}}}
           (apply-entity-mod
             {:attrs {:5e/ability-score-increase {:dex 2}}}
             {:&attrs {:5e/&ability-score-increase {:int 1}}}))))

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
