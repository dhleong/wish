(ns wish.sheets.dnd5e.util-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sheets.dnd5e.util :refer [compile-multiclass-reqs
                                            post-process]]))

(defn ->abilities [abilities-partial]
  (merge {:str 1
          :dex 1
          :con 1
          :int 1
          :wis 1
          :cha 1}
         abilities-partial))

(deftest compile-multiclass-reqs-test
  (testing "Single stat"
    (is (true? ((compile-multiclass-reqs
                  {:str 13})
                (->abilities {:str 13}))))
    (is (false? ((compile-multiclass-reqs
                  {:str 13})
                (->abilities {:str 12}))))
    (is (false? ((compile-multiclass-reqs
                  {:str 13})
                (->abilities {:int 13})))))

  (testing "AND"
    (is (true? ((compile-multiclass-reqs
                  {:str 13
                   :dex 13})
                (->abilities {:str 13
                              :dex 13}))))
    (is (false? ((compile-multiclass-reqs
                  {:str 13
                   :dex 13})
                (->abilities {:str 12}))))
    (is (false? ((compile-multiclass-reqs
                  {:str 13
                   :dex 13})
                (->abilities {:str 13}))))
    (is (false? ((compile-multiclass-reqs
                  {:str 13
                   :dex 13})
                (->abilities {:dex 13})))))

  (testing "OR"
    (is (true? ((compile-multiclass-reqs
                  '({:str 13}
                    {:dex 13}))
                (->abilities {:str 13
                              :dex 13}))))
    (is (true? ((compile-multiclass-reqs
                  '({:str 13}
                    {:dex 13}))
                (->abilities {:str 13}))))
    (is (true? ((compile-multiclass-reqs
                  '({:str 13}
                    {:dex 13}))
                (->abilities {:dex 13}))))
    (is (false? ((compile-multiclass-reqs
                   '({:str 13}
                     {:dex 13}))
                 (->abilities {:dex 12}))))
    (is (false? ((compile-multiclass-reqs
                   '({:str 13}
                     {:dex 13}))
                 (->abilities {:int 13})))))

  (testing "Multi-AND / OR"
    (let [f (compile-multiclass-reqs
              '({:str 13
                 :int 13}
                {:dex 13}))]
      (is (true? (f (->abilities {:str 13
                                  :dex 13}))))
      (is (true? (f (->abilities {:str 13
                                  :int 13}))))
      (is (true? (f (->abilities {:dex 13}))))
      (is (false? (f (->abilities {:str 13}))))
      (is (false? (f (->abilities {:int 13})))))))


(deftest post-process-test
  (testing "Install spell slot limited-uses"
    (let [original {:attrs {:5e/spellcaster {}}}
          processed (post-process original nil :class)]
      (is (contains? (:limited-uses processed)
                     :slots/level-1))
      (is (= 4 ((-> processed :limited-uses
                    :slots/level-1
                    :restore-amount)
                {:used 4}))))))

