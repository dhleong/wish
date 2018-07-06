(ns wish.sheets.dnd5e.builder-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sheets.dnd5e.builder :refer [expanding-assoc
                                               expand-val]]))

(deftest expanding-assoc-test
  (testing "nil @0"
    (is (= [:mreynolds]
           (expanding-assoc nil 0 :mreynolds))))
  (testing "nil @1"
    (is (= [nil :mreynolds]
           (expanding-assoc nil 1 :mreynolds))))

  (testing "Empty vector @0"
    (is (= [:mreynolds]
           (expanding-assoc [] 0 :mreynolds))))
  (testing "Empty vector @1"
    (is (= [nil :mreynolds]
           (expanding-assoc [] 1 :mreynolds))))
  (testing "Empty vector @4"
    (is (= [nil nil nil nil :mreynolds]
           (expanding-assoc [] 4 :mreynolds))))

  (testing "Non-Empty vector @0"
    (is (= [:mreynolds]
           (expanding-assoc [:itskaylee] 0 :mreynolds))))
  (testing "Non-Empty vector @1"
    (is (= [:itskaylee :mreynolds]
           (expanding-assoc [:itskaylee] 1 :mreynolds))))
  (testing "Non-Empty vector @4"
    (is (= [:itskaylee nil nil nil :mreynolds]
           (expanding-assoc [:itskaylee] 4 :mreynolds)))))

(deftest expand-val-test
  (testing "Multi? Instanced? Feature @1"
    ; from scratch
    (is (= {:id :base-id
            :value [nil :dex]}
           (expand-val
             nil
             {:instanced? true
              :id :base-id}
             [:base-id#0 :value 1]

             :dex)))

    ; existing value @0
    (is (= {:id :base-id
            :value [:str :dex]}
           (expand-val
             {:id :base-id
              :value [:str]}
             {:instanced? true
              :id :base-id}
             [:base-id#0 :value 1]

             :dex)))))
