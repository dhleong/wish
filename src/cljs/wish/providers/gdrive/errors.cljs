(ns ^{:author "Daniel Leong"
      :doc "errors"}
  wish.providers.gdrive.errors
  (:require-macros [cljs.core.async :refer [go]]
                   [wish.util :refer [fn-click]]
                   [wish.util.log :as log :refer [log]])
  (:require [clojure.core.async :refer [<!]]
            [wish.providers.gdrive :as gdrive :refer [has-global-read?]]
            [wish.providers.gdrive.api :refer [view-file-link]]
            [wish.util :refer [>evt]]))

; ======= Permissions =====================================

(defn- request-file-access [id]
  [:<>
   [:div "If this file exists, you probably need to request access to it."]
   [:div.action
    [:a {:href (view-file-link id)
         :target "_blank"}
     "Click here to try to Request Access"]]
   [:div.metadata
    "Clicking the above link will open a new window. If there's a button labeled \"Request Access\" then the file exists and you can press that button to request the owner share it with you. Once you get an email saying they've shared it with you, you can click on \"Try Again\" below."]
   [:div.metadata
     "If there's no such button then, sadly, the file doesn't exist."]])

(defn- request-global-read []
  [:<>
   [:div "It may have been shared with you, but we'll need a bit more access
          to your Google Drive account in order to see it."]
   [:div
    [:a {:href "#"
         :on-click (fn-click
                     (log/info "Requesting read scope")
                     ; NOTE: returns [err, result]
                     (go (when-not (first (<! (gdrive/request-read!)))
                           ; on success, try again
                           (log "Granted read scope!")
                           (>evt [:retry-current-sheet!]))))}
     "Let's do it!"]]])

(defn resolve-permissions [id]
  [:div.error-resolver
   [:h4 "You may not have permission to view this file"]

   (if (has-global-read?)
     [request-file-access id]

     [request-global-read]) ])


; ======= Public interface ================================

(defn view
  [{:keys [id] :as data}]
  (cond
    (:permissions? data)
    [resolve-permissions id]))
