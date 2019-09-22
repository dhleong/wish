(ns wish.sheets.compiler-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish-engine.core :as engine]
            [wish.sheets.compiler :refer [compile-sheet decompile-sheet]]))

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
    (let [sheet (compile-sheet
                  {:engine (delay (engine/create-engine))}
                  `{:kind :dnd5e
                    :items {:custom/captains-coat ~captains-coat}})]
      (is (fn? (-> sheet :items :custom/captains-coat :!))))))

(deftest decompile-sheet-test
  (testing "Strip apply-fns"
    (let [compiled (compile-sheet
                     {:engine (delay (engine/create-engine))}
                     `{:kind :dnd5e
                       :items {:custom/captains-coat ~captains-coat}})
          decompiled (decompile-sheet nil compiled)]
      (is (fn? (-> compiled :items :custom/captains-coat :!)))
      (is (not (fn? (-> decompiled :items :custom/captains-coat :!))))
      (is (nil? (-> decompiled :items :custom/captains-coat :!-raw))))))
