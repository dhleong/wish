(ns wish.providers.gdrive.api-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [clojure.string :as str]
            [wish.providers.gdrive.api :refer [fix-unicode]]
            [goog.crypt.base64 :as b64]))

(defn- b64-encode [s]
  (-> s
      (js/encodeURIComponent)
      (str/replace #"%([0-9A-F]{2})"
                   (fn [[_ v]]
                     (js/String.fromCharCode (str "0x" v))))
      b64/encodeString))

(defn- fix-decode [s]
  (fix-unicode
    (js/atob s)))

(deftest fix-unicode-test
  (testing "Fix unicode munging"
    (is (not= "✓ à la mode"
              (js/atob "4pyTIMOgIGxhIG1vZGU=")))
    (is (= "✓ à la mode"
           (fix-decode
             "4pyTIMOgIGxhIG1vZGU=")))
    (is (= "\n"
           (fix-decode
             "Cg==")))
    (is (= "\n•"
           (fix-unicode "\n•"))))

  (testing "There and back"
    (let [original "✓ à la mode"]
      (is (= original
             (fix-decode
               (b64-encode original))))))

  (testing "Don't introduce new problems"
    (let [expected "{:id :ranger-rev/natural-explorer, :name \"Natural Explorer\", :desc \"You are a master of navigating the natural world, and you react with swift and decisive action when attacked. This grants you the following benefits:\n\n• You ignore difficult terrain.\n• You have advantage on initiative rolls.\n• On your first turn during combat, you have advantage on attack rolls against creatures that have not yet acted.\n\nIn addition, you are skilled at navigating the wilderness. You gain the following benefits when traveling for an hour or more:\n\n• Difficult terrain doesn’t slow your group’s travel.\n• Your group can’t become lost except by magical means.\n• Even when you are engaged in another activity while traveling (such as foraging, navigating, or tracking), you remain alert to danger.\n• If you are traveling alone, you can move stealthily at a normal pace.\n• When you forage, you find twice as much food as you normally would.\n• While tracking other creatures, you also learn their exact number, their sizes, and how long ago they passed through the area\"}"
          original "{:id :ranger-rev/natural-explorer, :name \"Natural Explorer\", :desc \"You are a master of navigating the natural world, and you react with swift and decisive action when attacked. This grants you the following benefits:\\n\\n• You ignore difficult terrain.\\n• You have advantage on initiative rolls.\\n• On your first turn during combat, you have advantage on attack rolls against creatures that have not yet acted.\\n\\nIn addition, you are skilled at navigating the wilderness. You gain the following benefits when traveling for an hour or more:\\n\\n• Difficult terrain doesn’t slow your group’s travel.\\n• Your group can’t become lost except by magical means.\\n• Even when you are engaged in another activity while traveling (such as foraging, navigating, or tracking), you remain alert to danger.\\n• If you are traveling alone, you can move stealthily at a normal pace.\\n• When you forage, you find twice as much food as you normally would.\\n• While tracking other creatures, you also learn their exact number, their sizes, and how long ago they passed through the area\"}"]
      (is (= expected
             (fix-unicode
               original))))))

