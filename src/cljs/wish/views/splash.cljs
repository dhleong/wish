(ns ^{:author "Daniel Leong"
      :doc "Splash screen"}
  wish.views.splash
  (:require [wish.config :as config]
            [wish.style :refer [defstyled]]
            [wish.style.flex :as flex :refer [flex]]))

(defstyled splash-container
  (merge flex
         flex/justify-center
         {:height "100vh"}))

(defstyled splash
  (merge flex/vertical-center
         flex/center)
  [:img.logo {:height "72px"
              :width "72px"
              :margin "12px"}])

(defn page []
  [:div splash-container
   [:div splash
    [:img.logo {:src (str config/server-root "/assets/icon/icon-192.png")}]
    "WISH"]])
