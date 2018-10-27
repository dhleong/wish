(ns wish.push-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.push :refer [session-args]]))

(deftest session-args-test
  (testing "Properly serializes sheet ids"
    (is (= "[\"gdrive/wid\"]"
           (js/JSON.stringify
             (clj->js
               (:ids
                 (session-args
                   {:gdrive "let-me-in"}
                   #{:gdrive/wid})))))))

  (testing "Removes ids with no provider auth"
    (is (= ["gdrive/wid"]
           (:ids
             (session-args
               {:gdrive "let-me-in"}
               [:gdrive/wid :wish/wid]))))))

