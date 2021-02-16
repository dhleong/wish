(ns wish.util.dice
  (:require [clojure.string :as str]))

(def ^:private die-spec-regex #"(\d+)[dD](\d+)[ ]*([+-]?[ ]*\d+)?")

(defn compute-average [dice-spec]
  (when-let [[_ die-count die-size modifier] (re-find die-spec-regex dice-spec)]
    (let [die-size (js/parseInt die-size)
          die-count (js/parseInt die-count)
          modifier (when modifier
                     (js/parseInt (str/replace modifier " " "")))
          size-average (Math/floor (/ (inc die-size) 2))]
      (+ (* size-average die-count)
         modifier))))
