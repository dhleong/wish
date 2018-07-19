(ns wish.sheets.dnd5e.events-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [wish.sheets.dnd5e.events :refer [update-hp-rolled
                                              update-hp]]))

(defn cofx
  "Stub the cofx map given a :sheet"
  [sheet]
  {:db
   {:page [:sheet :sheet-id]
    :sheets
    {:sheet-id sheet}}})

(defn get-in-sheet
  [cofx path]
  (get-in cofx (concat [:db :sheets :sheet-id]
                      path)))

(deftest update-hp-test
  (testing "Don't overheal"
    (let [updated (update-hp
                    (cofx
                      {:limited-uses
                       {:hp#uses 10}})
                    20 ; heal amt
                    20)] ; max hp
      (is (= {:hp#uses 0}
             (get-in-sheet
               updated
               [:limited-uses])))))

  (testing "Don't overdamage"
    (let [updated (update-hp
                    (cofx
                      {:limited-uses
                       {:hp#uses 10}})
                    -20 ; damage amt
                    20)] ; max hp
      (is (= {:hp#uses 20}
             (get-in-sheet
               updated
               [:limited-uses])))))

  (testing "Use ONLY temp HP"
    (let [updated (update-hp
                    (cofx
                      {:limited-uses {:hp#uses 10}
                       :sheet {:temp-hp 5}})
                    -3 ; damage amt
                    20)] ; max hp
      (is (= {:hp#uses 10}
             (get-in-sheet
               updated
               [:limited-uses])))
      (is (= {:temp-hp 2}
             (get-in-sheet
               updated
               [:sheet])))))

  (testing "Use temp HP first"
    (let [updated (update-hp
                    (cofx
                      {:limited-uses {:hp#uses 10}
                       :sheet {:temp-hp 5}})
                    -10 ; damage amt
                    20)] ; max hp
      (is (= {:hp#uses 15}
             (get-in-sheet
               updated
               [:limited-uses])))
      (is (= {:temp-hp 0}
             (get-in-sheet
               updated
               [:sheet])))))

  (testing "Don't affect temp-hp when healing"
    ; TODO
    ))

(deftest update-hp-rolled-test
  (testing "Existing vector + index"
    (is (= {:rogue [42]}
           (update-hp-rolled
             {:rogue [2]}
             [:rogue 0]
             42))))
  (testing "Existing vector, new index"
    (is (= {:rogue [2 42]}
           (update-hp-rolled
             {:rogue [2]}
             [:rogue 1]
             42))))
  (testing "No vector, 0 index"
    (is (= {:rogue [42]}
           (update-hp-rolled
             {}
             [:rogue 0]
             42))))
  (testing "No vector, > 0 index"
    (is (= {:rogue [nil nil 42]}
           (update-hp-rolled
             {}
             [:rogue 2]
             42))))
  (testing "Nil map, > 0 index"
    (is (= {:rogue [nil nil 42]}
           (update-hp-rolled
             nil
             [:rogue 2]
             42)))))

