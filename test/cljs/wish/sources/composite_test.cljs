(ns wish.sources.composite-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sources.compiler :as c :refer [compile-directives]]
            [wish.sources.core :as s :refer [->DataSource]]
            [wish.sources.composite :refer [composite-source]]))

(deftest composite-test
  (let [with-class (->DataSource :a (compile-directives
                                      [[:!declare-race
                                        {:id :human
                                         :attrs
                                         {:from :earth}
                                         :+features
                                         [{:id :feat/base}]}]]))
        with-sub (->DataSource :b (compile-directives
                                    [[:!declare-subrace
                                      :human
                                      {:id :human/variant
                                       :&attrs
                                       {:also :the-verse}
                                       :+features
                                       [{:id :feat/with-options
                                         :max-options 2}]}]]))
        combined (composite-source :ab [with-class
                                        with-sub])]

    (testing "Cross-source subraces resolve"
      (is (= {:id :human/variant
              :attrs {:from :earth
                      :also :the-verse}
              :subrace-of :human}

             (select-keys
               (s/find-race combined :human/variant)
               [:id :attrs :subrace-of])))

      (is (contains?
            (->> (s/list-entities combined :races)
                 (map :id)
                 (into #{}))
            :human/variant)))

    (testing "Features of cross-source subraces resolve and compile"
      (let [feature (->> (s/find-race combined :human/variant)
                         :features
                         :feat/with-options)]
        (is (identity feature))
        (is (fn? (:max-options feature))))

      ; NOTE: we should probably support finding racial features
      ; at some point, but things are working okay for now without
      ; supporting it for normal races, so I won't worry too hard
      ; about supporting it for cross-source subraces right now
      #_(is (identity
            (s/find-feature combined :feat/base)))
      #_(is (identity
            (s/find-feature combined :feat/with-options)))
      #_(is (fn?
            (:max-options
              (s/find-feature combined :feat/with-options)))))))

