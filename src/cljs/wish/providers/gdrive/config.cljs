(ns ^{:author "Daniel Leong"
      :doc "gdrive.config"}
  wish.providers.gdrive.config
  (:require [wish.providers.gdrive :as gdrive]
            [wish.util :refer [<sub]]))

(defn signin-prompt []
  [:div.button
   {:on-click gdrive/signin!}
   "Sign in"])

(defn connected-view []
  [:div "Connected!"
   [:div.button
    {:on-click gdrive/signout!}
    "Disconnect Google Account"]])

(defn view []
  (let [state (<sub [:provider-state :gdrive])]
    [:div
     [:h3 "Google Drive"]
     (case state
       :idle [:div "Preparing..."]
       :signed-out [signin-prompt]
       :signed-in [connected-view])
     ]))
