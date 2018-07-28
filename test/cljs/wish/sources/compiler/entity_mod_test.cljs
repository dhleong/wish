(ns wish.sources.compiler.entity-mod-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sources.compiler.entity-mod :refer [extract-mod-and-key
                                                      apply-entity-mod
                                                      merge-mods]]))

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
  (testing "Simple replace"
    (is (= {:restore-trigger :short-rest
            :other :value}
           (apply-entity-mod
             {:restore-trigger :long-rest
              :other :value}
             {:restore-trigger :short-rest}))))

  (testing "Explicit replace"
    (is (= {:&levels {3 {:+features [:foo]}}}
           (apply-entity-mod
             {}
             {:=&levels {3 {:+features [:foo]}}}))))

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
    (let [features-with-a (apply-entity-mod {} {:+features [:a]})]
      (is (= {:features {:a 1 :b {:id :b}}}
             (apply-entity-mod
               features-with-a
               {:+features {:b {:id :b}}})))
      (is (= {:features {:a 1 :b {:id :b}}}
             (apply-entity-mod
               features-with-a
               {:+features [{:id :b}]})))
      (is (= {:features {:a 1 :b 1}}
             (apply-entity-mod
               features-with-a
               {:+features [:b]})))))

  (testing "Multiple Feature instances"
    (let [features-with-a (apply-entity-mod {} {:+features [:a]})]
      (is (= {:features {:a 2}}
             (apply-entity-mod
               features-with-a
               {:+features [:a]}))))))

(deftest merge-mods-test
  (testing "Merge directives"
    (is (= [[:!add-to-list
             :list
             :2]
            [:!add-to-list
             :list
             :3]]
           (:+! (merge-mods
                  {:+! [[:!add-to-list
                         :list
                         :2]]}
                  {:+! [[:!add-to-list
                         :list
                         :3]]}))))))
