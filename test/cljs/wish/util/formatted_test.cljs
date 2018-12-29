(ns wish.util.formatted-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.util.formatted :refer [->hiccup]]))

(deftest basic-->hiccup-test
  (testing "Simple pass-through"
    (is (nil? (->hiccup nil)))
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

(deftest table->hiccup-test
  (testing "No headers table"
    (is (= [[:table
             [:tbody
              [:tr
               [:td "mreynolds"]
               [:td "wishwash"]]]]]

           (->hiccup
             {:rows [["mreynolds" "wishwash"]]}))))

  (testing "Table with headers"
    (is (= [[:table
             [:thead
              [:tr
               [:th "Captain"]
               [:th "Pilot"]]]
             [:tbody
              [:tr
               [:td "mreynolds"]
               [:td "wishwash"]]]]]

           (->hiccup
             {:headers ["Captain" "Pilot"]
              :rows [["mreynolds" "wishwash"]]}))))

  (testing "Formatted text in tables"
    (is (= [[:table
             [:thead
              [:tr
               [:th [:b "mreynolds"]]
               [:th [:i "wishwash"]]]]]]

           (->hiccup
             {:headers ["**mreynolds**" "_wishwash_"]})))))

(deftest vector->hiccup-test
  (testing "Vectors -> paragraphs"
    (is (= ["mreynolds" "itskaylee"]
           (->hiccup
             ["mreynolds", "itskaylee"]))))

  (testing "Table in vectors"
    (is (= ["Crew members:"
            [:table
             [:tbody
              [:tr
               [:td [:b "Captain"]]
               [:td "Mal Reynolds"]]
              [:tr
               [:td [:b "Pilot"]]
               [:td "Hoban Washburne"]]]]
            "Serenity"]

           (->hiccup
             ["Crew members:"
              {:rows [["**Captain**" "Mal Reynolds"]
                      ["**Pilot**" "Hoban Washburne"]]}
              "Serenity"])))))
