(ns ^{:author "Daniel Leong"
      :doc "errors"}
  wish.providers.gdrive.errors
  (:require-macros [cljs.core.async :refer [go]]
                   [wish.util :refer [fn-click]]
                   [wish.util.log :as log :refer [log]])
  (:require [clojure.core.async :refer [<!]]
            [wish.providers.gdrive :as gdrive
             :refer [has-global-read?]]
            [wish.util :refer [>evt]]))

(defn resolve-permissions [id]
  [:div.error-resolver
   [:h4 "You may not have permission to view this file"]

   (if (has-global-read?)
     [:div "TODO If this file exists, you probably need to request access to it."]

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
        "Let's do it!"]]]) ])

(defn view
  [{:keys [id] :as data}]
  (cond
    (:permissions? data)
    [resolve-permissions id]))
