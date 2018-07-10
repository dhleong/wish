(ns wish.util.formatted-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.util.formatted :refer [->hiccup]]))

(deftest basic-->hiccup-test
  (testing "Simple pass-through"
    (is (= ["world"]
           (->hiccup
             "world"))))
  (testing "Boldify"
    (is (= [[:b "itskaylee"]]
           (->hiccup
             "**itskaylee**")))
    (is (= [[:span [:b "its"] " " [:b "kaylee"]]]
           (->hiccup
             "**its** **kaylee**"))))
  (testing "Italicize"
    (is (= [[:i "itskaylee"]]
           (->hiccup
             "_itskaylee_")))
    (is (= [[:span [:i "its"] " " [:i "kaylee"]]]
           (->hiccup
             "_its_ _kaylee_"))))
  (testing "Bold+Italicize"
    (is (= [[:b [:i "itskaylee"]]]
           (->hiccup
             "**_itskaylee_**")))))

(deftest ul->hiccup-test
  (testing "Listify 1"
    (is (= [[:ul [:li.ul "itskaylee"]]]
           (->hiccup
             "- itskaylee"))))
  (testing "Listify with bold"
    (is (= [[:ul [:li.ul
                  "its"
                  [:b "kay"]
                  "lee"]]]
           (->hiccup
             "- its**kay**lee"))))
  (testing "Listify 2"
    (is (= [[:ul
             [:li.ul "itskaylee"]
             [:li.ul "mreynolds"]]]
           (->hiccup
             "- itskaylee\n- mreynolds")))))

(deftest ol->hiccup-test
  ;; NOTE: the fancy stuff is already tested
  ;; above for ul; these use the same mechanism,
  ;; so we just make sure it parses
  (testing "Listify 1"
    (is (= [[:ol [:li.ol "itskaylee"]]]
           (->hiccup
             "1. itskaylee")))))

