(ns ^{:author "Daniel Leong"
      :doc "String util fns"}
  wish.util.string
  (:require [clojure.string :as str]))

(defn includes-any-case?
  "Case-insensitive includes?"
  [s substr]
  ; NOTE: we could do this without generating strings,
  ; but the goog.string.caseInsensitiveContains doesn't
  ; bother with that so... meh
  (when (and s substr)
    (str/includes? (str/lower-case s)
                   (str/lower-case substr))))
