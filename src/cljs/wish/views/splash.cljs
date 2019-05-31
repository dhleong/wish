(ns ^{:author "Daniel Leong"
      :doc "Splash screen"}
  wish.views.splash
  (:require [wish.config :as config]))

(defn page []
  [:div#splash-container
   [:div.splash
    [:img.logo {:src (str config/server-root "/assets/icon/icon-192.png")}]
    "WISH"]])
