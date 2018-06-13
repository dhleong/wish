(ns ^{:author "Daniel Leong"
      :doc "Shared widgets"}
  wish.views.widgets
  (:require [clojure.string :as string]
            [wish.util :refer [<sub >evt click>evt]]
            [wish.util.nav :refer [pushy-supported?]]))

(defn icon
  "A material design icon. `spec` is a keyword
  that is usually the name of the icon, but
  can also have .class like normal hiccup"
  [spec]
  [(keyword (str "i.fa.fa-" (name spec)))])

(defn link
  "Drop-in replacement for :a that inserts the # in links if necessary"
  [attrs & contents]
  (if pushy-supported?
    (vec (concat [:a attrs] contents))
    (vec (concat [:a (update attrs
                             :href
                             (fn [s]
                               (str "#" s)))]
                 contents))))
