(ns wish.views.widgets.limited-select-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.views.widgets.limited-select :refer [toggle-option-set]]))

(deftest toggle-option-set-test
  (testing "Multi-select toggle"
    (is (= #{:mreynolds :zoe}
           (toggle-option-set #{:mreynolds} false :zoe)))
    (is (= #{:mreynolds}
           (toggle-option-set #{:mreynolds :zoe} false :zoe))))
  (testing "Single-select toggle"
    (is (= #{:zoe}
           (toggle-option-set #{:mreynolds} true :zoe)))
    (is (= #{}
           (toggle-option-set #{:zoe} true :zoe)))))

