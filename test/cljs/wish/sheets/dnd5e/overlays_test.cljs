(ns wish.sheets.dnd5e.overlays-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sheets.dnd5e.overlays :refer [expand-starting-eq
                                                install-limited-use]]))

(deftest install-limited-use-test
  (testing "Ignore when-not limited-use?"
    (is (= {}
           (install-limited-use {})))
    (is (= {}
           (install-limited-use {:limited-use? false
                                 :attunes? true
                                 :limited-use {:name "Name"}}))))

  (testing "Install with everything provided"
    (is (= {:id :serenity
            :attunes? true
            :! [[:!add-limited-use
                 {:id :serenity
                  :name "Crazy Ivan"
                  :uses 2
                  :restore-trigger :long-rest}]]}

           (install-limited-use {:id :serenity
                                 :limited-use? true
                                 :attunes? true
                                 :limited-use
                                 {:name "Crazy Ivan"
                                  :uses 2}}))))

  (testing "Install with nothing provided"
    (is (= {:id :serenity
            :name "Serenity"
            :! [[:!add-limited-use
                 {:id :serenity
                  :name "Serenity"
                  :uses 1
                  :restore-trigger :long-rest}]]}

           (install-limited-use {:id :serenity
                                 :name "Serenity"
                                 :limited-use? true})))))

(deftest expand-starting-eq-est
  (testing "Simple `and` UNselected"
    (is (= []
           (expand-starting-eq
             [[:and [{:id :mreynolds}]]]

             {0 {:chosen nil}}))))

  (testing "Simple `and` selected"
    (is (= [{:id :mreynolds}]
           (expand-starting-eq
             [[:and [{:id :mreynolds}]]]

             {0 {:chosen true}}))))

  (testing "Simple `and` with count"
    (is (= [[{:id :bobble-geisha} 10]]
           (expand-starting-eq
             [[:and [[:count {:id :bobble-geisha} 10]]]]

             {0 {:chosen true}}))))

  (testing "Single alternative"
    (is (= []
           (expand-starting-eq
             '([:or [{:id :mreynolds} {:id :yosafbridge}]])

             ; *nothing* chosen
             {0 {:chosen nil}})))

    (is (= [{:id :mreynolds}]
           (expand-starting-eq
             '([:or [{:id :mreynolds} {:id :yosafbridge}]])

             {0 {:chosen 0}})))

    (is (= [{:id :yosafbridge}]
           (expand-starting-eq
             '([:or [{:id :mreynolds} {:id :yosafbridge}]])

             {0 {:chosen 1}}))))

  (testing "Single alternative with group"
    (is (= [{:id :zoe} {:id :mreynolds}]
           (expand-starting-eq
             '([:or [[:and [{:id :zoe} {:id :mreynolds}]]
                     {:id :yosafbridge}]])

             {0 {:chosen 0}}))))

  (testing "Nested :or"
    (let [choices '([:or [[:or [{:id :zoe} {:id :mreynolds}]]
                          {:id :yosafbridge}]])]
      (is (= [{:id :zoe}]
             (expand-starting-eq
               choices

               {0 {:chosen 0}})))

      (is (= [{:id :mreynolds}]
             (expand-starting-eq
               choices

               {0 {:chosen 0, 0 1}})))))

  (testing "Single alternative with pack"
    (is (= [{:id :mreynolds}
            {:id :zoe}]

           (expand-starting-eq
             '([:or [:a-pack
                     [:pack {:name "Crime Pack"
                             :contents [{:id :mreynolds}
                                        {:id :zoe}]}]]])
             {0 {:chosen 1}}))))

  (testing "`and` with a choice"
    (let [choices '([:and [{:id :yosafbridge}
                           [:or [{:id :zoe} {:id :mreynolds}]]]])]
      ; a choice was made but later de-selected
      (is (= []
             (expand-starting-eq
               choices

               {0 {:chosen nil, 1 1}})))

      ; normal case; a choice has been made
      (is (= [{:id :yosafbridge} {:id :mreynolds}]
             (expand-starting-eq
               choices

               {0 {:chosen true, 1 1}})))

      ; if there's no explicit choice, default to the first one
      (is (= [{:id :yosafbridge} {:id :zoe}]
             (expand-starting-eq
               choices

               {0 {:chosen true}})))))

  (testing "Or in And in Or"
    (is (= [{:id :yosafbridge} {:id :zoe}]
           (expand-starting-eq
             '([:or [{:id :badger}
                     [:and [{:id :yosafbridge}
                            [:or [{:id :mreynolds}
                                  {:id :zoe}]]]]]])

             {0 {:chosen 1, 1 {1 1}}})))))
