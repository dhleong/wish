(ns wish.views
  (:require
   [re-frame.core :as re-frame]
   [re-pressed.core :as rp]
   [wish.subs :as subs]
   [wish.util :refer [<sub]]
   [wish.views.router :refer [router]]
   [wish.views.widgets :refer [link]]
   ))


;; home

(defn display-re-pressed-example []
  (let [re-pressed-example (re-frame/subscribe [::subs/re-pressed-example])]
    [:div

     [:p
      [:span "Re-pressed is listening for keydown events. A message will be displayed when you type "]
      [:strong [:code "hello"]]
      [:span ". So go ahead, try it out!"]]

     (when-let [rpe @re-pressed-example]
       [:div
        {:style {:padding          "16px"
                 :background-color "lightgrey"
                 :border           "solid 1px grey"
                 :border-radius    "4px"
                 :margin-top       "16px"
                 }}
        rpe])]))

(defn home-panel []
  [:div
   [:h1 (str "Hello. This is the Home Page.")]

   [:div
    [link {:href "/about"}
     "go to About Page"]]

   [display-re-pressed-example]
   ])


;; about

(defn about-panel []
  [:div
   [:h1 "This is the About Page."]

   [:div
    [link {:href "/"}
     "go to Home Page"]]])


;; main

(def pages
  {:home #'home-panel
   :about #'about-panel})

(defn main []
  [:div#main
   [router pages]])
