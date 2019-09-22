(ns wish.views.sheet-builder-util-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.views.sheet-builder-util :refer [count-max-options]]))

(deftest count-max-options-test
  (testing "Hard-coded :max-options"
    (is (= 2
           (count-max-options
             {:values [:a :b :c]
              :max-options 2}))))

  (testing "fn-based :max-options"
    (let [compiled (fn [{:keys [features]}]
                     (<= (count features) 3))]
      (is (= 3
             (count-max-options
               {:values [:a :b :c :d]
                :max-options compiled}))))))

