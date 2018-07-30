(ns wish.subs-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sources.compiler :refer [compile-directives]]
            [wish.sources.core :as src :refer [->DataSource]]
            [wish.subs :refer [inflate-option-values]]))

(defn ->ds
  [& directives]
  (->DataSource
    :src
    (compile-directives directives))  )

(defn inflate-option-values->ids
  [data-source options feature-id values]
  (->> (inflate-option-values
         data-source options feature-id values)
       (map #(select-keys % [:id]))))

(deftest inflate-option-values-test
  (testing "Inflate chosen options from feature"
    (is (= [{:id :bobble-geisha}]

           (inflate-option-values->ids
             (->ds [:!add-to-list
                    :cargo
                    {:id :bobble-geisha}])
             {:shipment [:bobble-geisha]}
             :to-load
             [:shipment>>options])))))

