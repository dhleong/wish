(ns ^{:author "Daniel Leong"
      :doc "errors"}
  wish.providers.gdrive.errors
  (:require-macros [cljs.core.async :refer [go]]
                   [wish.util :refer [fn-click]]
                   [wish.util.log :as log :refer [log]])
  (:require [clojure.core.async :refer [<!]]
            [reagent.core :as r]
            [wish.providers.gdrive :as gdrive :refer [has-global-read?]]
            [wish.providers.gdrive.api :refer [view-file-link]]
            [wish.providers.gdrive.config :refer [signin-prompt]]
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

(defn- request-global-read [& [specific-file?]]
  (r/with-let [requested? (r/atom false)]
    [:<>
     (when specific-file?
       [:div "It may have been shared with you, but we'll need a bit more access
              to your Google Drive account in order to see it."])

     [:div
      [:a {:href "#"
           :on-click (fn-click
                       (log/info "Requesting read scope")
                       (reset! requested? true)
                       ; NOTE: returns [err, result]
                       (go (when-not (first (<! (gdrive/request-read!)))
                             ; on success, try again
                             (log "Granted read scope!")
                             (if specific-file?
                               (>evt [:retry-current-sheet!])
                               (>evt [:providers/query-sheets :gdrive])))))}
       (if specific-file?
         "Let's do it!"
         "Grant access to browse shared sheets")]]

     (when @requested?
       [:<>
        [:div.metadata "A window should have opened with the permission request.
                        If not, your browser may have blocked it. You'll need to find the button to open it."]
        [:div.metadata "On Chrome, that button is in the upper-right corner of the browser, near the address bar."]])]))

(defn resolve-permissions [id]
  [:div.error-resolver
   [:h4 "You may not have permission to view this file"]

   (if (has-global-read?)
     [request-file-access id]

     [request-global-read :specific-file!]) ])

(defn resolve-signed-out []
  [:div.error-resolver
   [:h4 "You're not signed into Google Drive"]
   [:div "You'll need to sign in to view this file"]
   [signin-prompt]
   ])

; ======= Public interface ================================

(defn view
  [{:keys [id state] :as data}]
  (cond
    (= :unavailable state)
    [:div.error-resolver
     [:h4 "Google Drive is not available right now"]]

    (= :no-shared-sheets state)
    [request-global-read]

    (= :signed-out state)
    [resolve-signed-out]

    (:permissions? data)
    [resolve-permissions id]))
