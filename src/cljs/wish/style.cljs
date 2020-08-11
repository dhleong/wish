(ns ^{:author "Daniel Leong"
      :doc "style"}
  wish.style
  (:require [spade.core :refer [defglobal]]
            [wish.config :refer [server-root]]
            [wish.style.components]
            [wish.style.media :as media]))

(def text-primary-on-light "#191d24")
(def text-primary-on-dark "#f4f7ff")

(defglobal global-styles
  (at-media media/dark-scheme
    [:body {:background "#000"
            :color text-primary-on-dark}]
    [:input :textarea :select {:background-color "#444"
                               :color text-primary-on-dark}]))

(defn asset [n]
  (str server-root "/assets/" n))

