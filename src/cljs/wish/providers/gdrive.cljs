(ns ^{:author "Daniel Leong"
      :doc "Google-drive powered Provider"}
  wish.providers.gdrive
  (:require-macros [cljs.core.async :refer [go]]
                   [wish.util.async :refer [call-with-cb->chan]])
  (:require [clojure.core.async :refer [chan put! to-chan <! >!]]
            [clojure.string :as str]
            [cljs.reader :as edn]
            [wish.providers.core :refer [IProvider]]
            [wish.sheets.util :refer [make-id]]
            [wish.util :refer [>evt]]))


;;
;; Constants
;;

;; Client ID and API key from the Developer Console
(def client-id "661182319990-3aa8akj9fh8eva9lf7bt02621q2i18s6.apps.googleusercontent.com")

;; Array of API discovery doc URLs for APIs used by the quickstart
(def discovery-docs #js ["https://www.googleapis.com/discovery/v1/apis/drive/v3/rest"])

;; Authorization scopes required by the API; multiple scopes can be
;; included, separated by spaces.
(def scopes (str/join
              " "
              ["https://www.googleapis.com/auth/drive.appfolder"
               "https://www.googleapis.com/auth/drive.appdata"
               "https://www.googleapis.com/auth/drive.file"]))

;;
;; Internal util
;;

(defn- ->id
  [gapi-id]
  (keyword "gdrive" gapi-id))

(defn- log
  [& args]
  (apply js/console.log "[gdrive]" args))

(defn- log+warn
  [& args]
  (apply js/console.warn "[gdrive]" args))

(defn- log+err
  [& args]
  (apply js/console.error "[gdrive]" args))



;;
;; State management and API interactions
;;

(defn- auth-instance
  "Convenience to get the gapi auth instance:
   gapi.auth2.getAuthInstance().
   @return {gapi.AuthInstance}"
  []
  (js/gapi.auth2.getAuthInstance))

(declare on-files-list)
(defn- update-signin-status!
  [signed-in?]
  (log "signed-in? <-" signed-in?)
  (>evt [:put-provider-state! :gdrive (if signed-in?
                                        :signed-in
                                        :signed-out)])
  (when signed-in?
    (>evt [:mark-provider-listing! :gdrive true])
    (-> js/gapi.client.drive.files
        (.list #js {:q "appProperties has { key='wish-type' and value='wish-sheet' }"
                    :pageSize 50
                    :spaces "drive,appDataFolder"
                    :fields "nextPageToken, files(id, name)"})
        (.then on-files-list
               (fn [e]
                 (log+err "ERROR listing files" e))))))

(defn on-files-list
  [response]
  (log "FILES LIST:" response)
  (let [response (js->clj response :keywordize-keys true)
        files (->> response
                   :result
                   :files
                   (map
                     (fn [raw-file]
                       [(make-id :gdrive (:id raw-file))
                        (select-keys raw-file
                                     [:name])])))]
    (log "Found: " files)
    (>evt [:add-sheets files])
    (>evt [:mark-provider-listing! :gdrive false])))

(defn- on-client-init
  []
  (log "gapi client init!")
  ; listen for updates
  (-> (auth-instance)
      (.-isSignedIn)
      (.listen update-signin-status!))
  ; set current status immediately
  (update-signin-status!
    (-> (auth-instance)
        (.-isSignedIn)
        (.get))))

(defn init-client!
  []
  (-> (js/gapi.client.init
        #js {:discoveryDocs discovery-docs
             :clientId client-id
             :scope scopes})
      (.then on-client-init)))

;;
;; NOTE: Exposed to index.html
(defn ^:export handle-client-load
  []
  (log "load")
  (js/gapi.load "client:auth2", init-client!))

;;
;; Public API
;;

(defn signin!
  []
  (-> (auth-instance)
      (.signIn)))

(defn signout!
  []
  (-> (auth-instance)
      (.signOut)))

(defn upload-data
  "The GAPI client doesn't provide proper support for
   file uploads out-of-the-box, so let's roll our own.

   Returns a channel that emits [err, res], where err
   is non-nil on error, and res is non-nil on success."
  [upload-type metadata content]
  {:pre [(contains? #{:create :update} upload-type)
         (string? (:mimeType metadata))
         (not (nil? content))]}
  (let [base (case upload-type
               :create {:path "/upload/drive/v3/files"
                        :method "POST"}
               :update {:path (str "/upload/drive/v3/files/"
                                   (:fileId metadata))
                        :method "PATCH"})
        boundary "-------314159265358979323846"
        delimiter (str "\r\n--" boundary "\r\n")
        close-delim (str "\r\n--" boundary "--")
        body (str delimiter
                  "Content-Type: application/json\r\n\r\n"
                  (js/JSON.stringify (clj->js metadata))
                  delimiter
                  "Content-Type: " (:mimeType metadata) "\r\n\r\n"
                  content
                  close-delim)
        request (assoc base
                       :params {:uploadType "multipart"}
                       :headers
                       {:Content-Type
                        (str "multipart/related; boundary=\"" boundary "\"")}
                       :body body)]
    (call-with-cb->chan
      (.execute
        (js/gapi.client.request
          (clj->js request))))))

(defn- refresh-auth []
  (call-with-cb->chan
    (js/gapi.auth.authorize
      #js {:client_id client-id
           :scope scopes
           :immediate true})))

(defn upload-data-with-retry
  [upload-type metadata content]
  (go (let [[error resp] (<! (upload-data
                               upload-type metadata content))]
        (cond
          (and error
               (= 401 (.-code error)))
          ; refresh creds and retry
          (let [_ (log "Refreshing auth before retrying upload...")
                [refresh-err refresh-resp] (<! (refresh-auth))]
            (if refresh-err
              (do
                ;; TODO notify?
                (log+warn "Auth refresh failed:" refresh-resp)
                [refresh-err nil])

              (let [_ (log "Auth refreshed! Retrying upload...")
                    [retry-err retry-resp] (<! (upload-data
                                                 upload-type metadata content))]
                (if retry-err
                  (do
                    (log+err "Even after auth refresh, upload failed: " resp)
                    [retry-err nil])

                  ; upload retry succeeded!
                  [nil retry-resp]))))

          ; unexpected error:
          error (do
                  (log+err "upload-data ERROR:" error)
                  [error nil])

          ; no problem; pass it along
          :else [nil resp]))))

(deftype GDriveProvider []
  IProvider
  (create-sheet [this file-name data]
    (log "Create sheet " file-name)
    (go (let [[err resp] (<! (upload-data-with-retry
                               :create
                               {:name file-name
                                :mimeType "application/edn"
                                :appProperties {:wish-type "wish-sheet"}}
                               (str data)))]
          (if err
            [err nil]

            (let [pro-sheet-id (:id resp)]
              (log "CREATED" resp)
              [nil (make-id :gdrive pro-sheet-id)])))))

  #_(delete-sheet [this info]
      (log "Delete " (:gapi-id info))
      (-> js/gapi.client.drive.files
          (.delete #js {:fileId (:gapi-id info)})
          (.then (fn [resp]
                   (log "Deleted!" (:gapi-id info)))
                 (fn [e]
                   (log+warn "Failed to delete " (:gapi-id info))))))

  #_(refresh-sheet [this info on-complete]
      (log "Refresh " (:gapi-id info))
      (-> js/gapi.client.drive.files
          (.get #js {:fileId (:gapi-id info)
                     :alt "media"})
          (.then (fn [resp]
                   (log "REFRESH resp" resp)
                   (when-let [body (.-body resp)]
                     (when-let [data (edn/read-string body)]
                       (on-complete data))))
                 (fn [e]
                   ;; TODO
                   (log+err "ERROR listing files" e)))))

  (init! [this]) ; nop

  (load-raw
    [this id]
    (to-chan [[(js/Error. "Not implemented") nil]]))
  (load-sheet
    [this id]
    (to-chan [[(js/Error. "Not implemented") nil]]))

  (save-sheet [this file-id data]
    (log "Save " file-id)
    (log (str data))
    (upload-data-with-retry
      :update
      {:fileId file-id
       :mimeType "application/json"}
      (str data))))

(defn create-provider []
  (->GDriveProvider))
