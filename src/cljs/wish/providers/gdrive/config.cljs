(ns ^{:author "Daniel Leong"
      :doc "gdrive.config"}
  wish.providers.gdrive.config
  (:require [wish.providers.gdrive :as gdrive]
            [wish.views.widgets :refer [link]]
            [wish.util :refer [<sub]]))

(defn signin-prompt []
  [:div.button
   {:on-click gdrive/signin!}
   "Sign in"])

(defn connected-view []
  (let [user (gdrive/active-user)]
    [:div "Connected!"
     (when-let [n (:name user)]
       [:div
        "Welcome back, " n])

     [:div
      [link {:href "/sheets/new"}
       "Create a new sheet"]]
     [:div
      [link {:href "/sheets"}
       "Open a sheet"]]

     [:div.button
      {:on-click gdrive/signout!}

      [:div "Disconnect Google Account"]
      (when-let [e (:email user)]
        [:div "(" e ")"])]]))

(defn view []
  (let [state (<sub [:provider-state :gdrive])]
    [:div
     [:h3 "Google Drive"]
     (case state
       :idle [:div "Preparing..."]
       :signed-out [signin-prompt]
       :signed-in [connected-view])
     ]))
