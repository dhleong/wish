(ns wish.style-test
  (:require [clojure.test :refer :all]
            [wish.style :refer [insert-root]]))

(deftest insert-root-test
  (testing "Basic root insert"
    (is (= '[[:.root [:ship] [:cargo]]]
           (insert-root
             :.root
             [[:ship]
              [:cargo]]))))

  (testing "Single at-media"
    (is (= ['(at-media my-media
                       [:.root [:ship]])
             [:.root [:ship] [:cargo]]]

           (insert-root
             :.root
             ['(at-media my-media
                          [:ship])
              [:ship]
              [:cargo]]))))

  (testing "Basic Self-styling"
    (is (= [[:.root
             {:ship :firefly}
             [:.cargo]]]

           (insert-root
             :.root
             [{:ship :firefly}
              [:.cargo]]))))

  (testing "(at-media) with Self-styling"
    (is (= ['(at-media 'my-media
                        [:.root
                         {:ship :firefly}])
            [:.root
             [:.cargo]]]

           (insert-root
             :.root
             ['(at-media 'my-media
                         {:ship :firefly})
              [:.cargo]])))))



