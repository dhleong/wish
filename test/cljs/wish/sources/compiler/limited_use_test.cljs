(ns wish.sources.compiler.limited-use-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [cljs.nodejs :as node]
            [wish.sources.compiler.limited-use :refer [compile-limited-use]]))

(deftest compile-test
  (testing "Constant restore-amounts"
    (let [f (:restore-amount
              (compile-limited-use
                {:uses 1
                 :restore-amount 1}))]
      (is (= 1 (f {:used 2})))))

  (testing "Restore amount defaults to 'all'"
    (let [f (:restore-amount
              (compile-limited-use
                {:uses 1}))]
      (is (= 2 (f {:used 2}))))))

