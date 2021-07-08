(ns wish.style
  (:require [garden.color :as color]
            [spade.core :refer [defglobal]]
            [wish.config :refer [server-root]]
            [wish.style.components]
            [wish.style.media :as media]))

(def text-primary-on-light "#191d24")
(def text-primary-on-dark "#E0EBFF")
(def header-primary-on-dark "#F4F7FF")

(def link-color "#fbc02d")

(defglobal global-styles
  (at-media media/dark-scheme
    [:body {:background "#000"
            :color text-primary-on-dark}]
    [:input :textarea :select {:background-color "#444"
                               :color text-primary-on-dark}]
    [:h1 :h2 :h3 :h4 :h5 {:color header-primary-on-dark}])

  [:a {:color link-color
       :text-decoration :none}
   [:&:hover {:color (color/lighten link-color 0.20)}]]

  )

(defn asset [n]
  (str server-root "/assets/" n))

