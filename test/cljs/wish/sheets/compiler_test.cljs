(ns wish.sheets.compiler-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish-engine.core :as engine]
            [wish.sheets.compiler :as compiler]))

(def captains-coat
  '{:type :gear,
    :name "Captain's Coat", :attunes? true,
    :desc "**_Captain's Coat_** Once a day, you can do something super awesome",
    :id :custom/captains-coat,
    :! (on-state
         (add-limited-use
           {:id :custom/captains-coat,
            :name "Captain's Coat",
            :uses 1,
            :restore-trigger :long-rest})),
    :notes "It's brown"})

(deftest compile-sheet-test
  (testing "Inflate apply-fns on custom items"
    (let [items (compiler/sheet-items
                  (engine/create-engine)
                  `{:custom/captains-coat ~captains-coat})]
      (is (fn? (-> items :custom/captains-coat :!))))))
