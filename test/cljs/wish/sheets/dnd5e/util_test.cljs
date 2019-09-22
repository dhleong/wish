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
    (is (nil? ((compile-multiclass-reqs
                  {:str 13})
                (->abilities {:str 13}))))
    (is (= ((compile-multiclass-reqs
              {:str 13})
            (->abilities {:str 12}))
           "STR 13 (is: 12)"))
    (is (= ((compile-multiclass-reqs
              {:str 13})
            (->abilities {:int 13}))
           "STR 13 (is: 1)")))

  (testing "AND"
    (is (nil? ((compile-multiclass-reqs
                 {:str 13
                  :dex 13})
               (->abilities {:str 13
                             :dex 13}))))
    (is (= ((compile-multiclass-reqs
              {:str 13
               :dex 13})
            (->abilities {:str 12}))
           "STR 13 (is: 12)"))
    (is (= ((compile-multiclass-reqs
              {:str 13
               :dex 13})
            (->abilities {:str 13}))
           "DEX 13 (is: 1)"))
    (is (= ((compile-multiclass-reqs
              {:str 13
               :dex 13})
            (->abilities {:dex 13}))
           "STR 13 (is: 1)")))

  (testing "OR"
    (is (nil? ((compile-multiclass-reqs
                 '({:str 13}
                   {:dex 13}))
               (->abilities {:str 13
                             :dex 13}))))
    (is (nil? ((compile-multiclass-reqs
                 '({:str 13}
                   {:dex 13}))
               (->abilities {:str 13}))))
    (is (nil? ((compile-multiclass-reqs
                 '({:str 13}
                   {:dex 13}))
               (->abilities {:dex 13}))))
    (is (= ((compile-multiclass-reqs
              '({:str 13}
                {:dex 13}))
            (->abilities {:dex 12}))
           "None of: STR 13 (is: 1), or DEX 13 (is: 12)"))
    (is (= ((compile-multiclass-reqs
              '({:str 13}
                {:dex 13}))
            (->abilities {:int 13}))
           "None of: STR 13 (is: 1), or DEX 13 (is: 1)")))

  (testing "Multi-AND / OR"
    (let [f (compile-multiclass-reqs
              '({:str 13
                 :int 13}
                {:dex 13}))]
      (is (nil? (f (->abilities {:str 13
                                  :dex 13}))))
      (is (nil? (f (->abilities {:str 13
                                  :int 13}))))
      (is (nil? (f (->abilities {:dex 13}))))

      ; NOTE: we could stand to improve the explanation a bit here,
      ; but I don't think this is commonly a thing, so... whatever
      (is (string? (f (->abilities {:str 13}))))
      (is (string? (f (->abilities {:int 13})))))))


(deftest post-process-test
  (testing "Install spell slot limited-uses"
    (let [original {:attrs {:5e/spellcaster {}}}
          processed (post-process original)]
      (is (contains? (:limited-uses processed)
                     :slots/level-1))
      (is (= 4 ((-> processed :limited-uses
                    :slots/level-1
                    :restore-amount)
                {:used 4}))))))

