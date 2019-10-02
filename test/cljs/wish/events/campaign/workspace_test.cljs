(ns wish.events.campaign.workspace-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.events.campaign.workspace :refer [apply-drag]]))

(deftest apply-drag-test
  (testing "Reorder within same secondary"
    (is (= {:s/serenity
            {:s [:first :second]}}

           (apply-drag
             {:s/serenity
              {:s [:second :first]}}
             {:item :first
              :from [:s/serenity :secondary 1]
              :to [:s/serenity :secondary 0]})))))

