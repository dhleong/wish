(ns wish.sheets.dnd5e.data-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sheets.dnd5e.data :refer [inflate-armor
                                            inflate-weapon]]))

(defn eval-ac
  [item ctx]
  (let [f (-> item :attrs :5e/ac vals first)]
    (f ctx)))

(deftest inflate-armor-test
  (testing "AC from modifier"
    (let [inflated (inflate-armor
                     {:kind :scale-mail})]
      (is (= 15
             (eval-ac
               inflated
               {:modifiers {:dex 1}})))))

  (testing "AC from modifier with a bonus"
    (let [inflated (inflate-armor
                     {:kind :scale-mail
                      :+ 2})]
      (is (= 17
             (eval-ac
               inflated
               {:modifiers {:dex 1}})))))

  (testing "AC buff (shields)"
    (let [inflated (inflate-armor
                     {:id :shield
                      :kind :shield})]
      (is (= {:shield 2}
             (-> inflated :attrs :buffs :ac))))))

