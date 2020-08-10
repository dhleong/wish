(ns ^{:author "Daniel Leong"
      :doc "style"}
  wish.style
  (:require [spade.core :refer [defglobal]]
            [wish.config :refer [server-root]]
            [wish.style.media :as media]))

(defglobal global-styles
  (at-media media/dark-scheme
    [:body {:background "#191d24"
            :color "#f4f7ff"}]))

(defn asset [n]
  (str server-root "/assets/" n))

