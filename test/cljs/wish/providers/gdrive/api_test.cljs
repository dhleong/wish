(ns wish.providers.gdrive.api-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.providers.gdrive.api :refer [fix-unicode]]))

(deftest fix-unicode-test
  (testing "Fix unicode munging"
    (is (not= "✓ à la mode"
              (js/atob "4pyTIMOgIGxhIG1vZGU=")))
    (is (= "✓ à la mode"
           (fix-unicode
             (js/atob "4pyTIMOgIGxhIG1vZGU="))))))

