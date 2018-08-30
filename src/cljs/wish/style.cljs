(ns ^{:author "Daniel Leong"
      :doc "style"}
  wish.style
  (:require-macros [wish.style])
  (:require [cljs-css-modules.macro :as css]  ; required by macros in .cljc
            [wish.config :refer [server-root]]))

(defn asset [n]
  (str server-root "/assets/" n))

