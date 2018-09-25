(ns wish.sources.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sources.compiler :as c :refer [compile-directives]]
            [wish.sources.core :as s :refer [composite ->DataSource]]))

(deftest composite-test
  (testing "Resolve cross-source subrace"
    (let [with-class (->DataSource :a (compile-directives
                                        [[:!declare-race
                                          {:id :human
                                           :attrs
                                           {:from :earth}}]]))
          with-sub (->DataSource :b (compile-directives
                                      [[:!declare-subrace
                                        :human
                                        {:id :human/variant
                                         :&attrs
                                         {:also :the-verse}}]]))
          combined (composite :ab [with-class
                                   with-sub])]
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
            :human/variant)))))

