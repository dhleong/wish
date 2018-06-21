(ns ^{:author "Daniel Leong"
      :doc "Google-drive powered Provider"}
  wish.providers.gdrive
  (:require [clojure.core.async :refer [chan put! to-chan]]
            [clojure.string :as str]
            [cljs.reader :as edn]
            [wish.providers.core :refer [IProvider]]))


;;
;; Constants
;;

;; Client ID and API key from the Developer Console
(def client-id "772789905450-0lur5kbi666jno4uplvd1e4g6c52a690.apps.googleusercontent.com")

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

(defn ->id
  [gapi-id]
  (keyword "gdrive" gapi-id))

(defn ->sheet
  [gapi-id sheet-name]
  {:provider :gapi
   :id (->id gapi-id)
   :name sheet-name
   :gapi-id gapi-id})

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
  (println "signed-in? <-" signed-in?)
  ;; (dispatch [:assoc-provider! :gapi :ready? signed-in?])
  (when signed-in?
    ;; (dispatch [:mark-loading! :gapi true])
    (-> js/gapi.client.drive.files
        (.list #js {:q "appProperties has { key='wish-type' and value='wish-sheet' }"
                    :pageSize 50
                    :spaces "drive,appDataFolder"
                    :fields "nextPageToken, files(id, name)"})
        (.then on-files-list
               (fn [e]
                 (println "ERROR listing files" e))))))

(defn on-files-list
  [response]
  (js/console.log "FILES LIST:" response)
  (let [response (js->clj response :keywordize-keys true)
        files (->> response
                   :result
                   :files
                   (map
                     (fn [raw-file]
                       (->sheet (:id raw-file)
                                (:name raw-file)))))]
    (println "Found: " files)
    ;; (dispatch [:add-sheets files])
    ;; (dispatch [:mark-loading! :gapi false])
    ))

(defn- on-client-init
  []
  (js/console.log "gapi client init!")
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
  (js/console.log "init-client!")
  (-> (js/gapi.client.init
        #js {:discoveryDocs discovery-docs
             :clientId client-id
             :scope scopes})
      (.then on-client-init)))

;;
;; NOTE: Exposed to index.html
(defn ^:export handle-client-load
  []
  (js/console.log "handle-client-load")
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
  file uploads out-of-the-box, so let's roll our own"
  [upload-type metadata content on-complete]
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
    (.execute
      (js/gapi.client.request
        (clj->js request))
      on-complete)))

(defn upload-data-with-retry
  [upload-type metadata content on-complete]
  (upload-data
    upload-type metadata content
    (fn [resp]
      (let [error (.-error resp)]
        (cond
          (and error
               (= 401 (.-code error)))
          ; refresh creds and retry
          (do
            (js/console.log "Refreshing auth before retrying upload...")
            (js/gapi.auth.authorize
              #js {:client_id client-id
                   :scope scopes
                   :immediate true}
              (fn [refresh-resp]
                (if (.-error refresh-resp)
                  (do
                    ;; TODO notify?
                    (js/console.warn "Auth refresh failed:" refresh-resp)
                    (on-complete nil))
                  (do
                    (js/console.log "Auth refreshed! Retrying upload...")
                    (upload-data
                      upload-type metadata content
                      (fn [resp]
                        (if (.-error resp)
                          (do
                            (js/console.error "Even after auth refresh, upload failed: " resp)
                            (on-complete nil))
                          (on-complete resp)))))))))
          ; unexpected error:
          error (do
                  (js/console.error "upload-data ERROR:" error)
                  (on-complete nil))
          ; no problem; pass it along
          :else (on-complete resp))))))

(deftype GDriveProvider []
  IProvider
  ;; (create-sheet [this info on-complete]
  ;;   (upload-data-with-retry
  ;;     :create
  ;;     {:name (:name info)
  ;;      :mimeType "application/json"
  ;;      :parents ["appDataFolder"]}
  ;;     (str
  ;;       (if-let [template (:template info)]
  ;;         ; normal case; create using the template
  ;;         (assoc template
  ;;                :name (:name info))
  ;;         ; shouldn't happen anymore; create an empty sheet
  ;;         (do
  ;;           (js/console.warn "No template data provided...")
  ;;           {:name (:name info)
  ;;            :pages
  ;;            [{:name "Main"
  ;;              :spec [:div "Coming soon!"]}
  ;;             {:name "Notes"
  ;;              :type :notes}]})))
  ;;     (fn [response]
  ;;       (if response
  ;;         (let [id (-> response
  ;;                      (js->clj :keywordize-keys true)
  ;;                      :id)]
  ;;           (when js/goog.DEBUG
  ;;             (println "CREATED:" response))
  ;;           (on-complete
  ;;             (->sheet id
  ;;                      (:name info))))
  ;;         (do
  ;;           ;; TODO: notify user
  ;;           (js/console.error "Failed to create sheet"))))))


  #_(delete-sheet [this info]
      (println "Delete " (:gapi-id info))
      (-> js/gapi.client.drive.files
          (.delete #js {:fileId (:gapi-id info)})
          (.then (fn [resp]
                   (println "Deleted!" (:gapi-id info)))
                 (fn [e]
                   (js/console.warn "Failed to delete " (:gapi-id info))))))

  #_(refresh-sheet [this info on-complete]
      (println "Refresh " (:gapi-id info))
      (-> js/gapi.client.drive.files
          (.get #js {:fileId (:gapi-id info)
                     :alt "media"})
          (.then (fn [resp]
                   (js/console.log resp)
                   (when-let [body (.-body resp)]
                     (when-let [data (edn/read-string body)]
                       (on-complete data))))
                 (fn [e]
                   ;; TODO
                   (println "ERROR listing files" e)))))

  (init! [this]) ; nop

  (load-raw
    [this id]
    (to-chan [[(js/Error. "Not implemented") nil]]))
  (load-sheet
    [this id]
    (to-chan [[(js/Error. "Not implemented") nil]]))

  (save-sheet [this file-id data]
    (let [ch (chan)]
      (println "Save " file-id)
      (println (str data))
      (upload-data-with-retry
        :update
        {:fileId file-id
         :mimeType "application/json"}
        (str data)
        (fn [response]
          (if response
            (do (println "SAVED!" response)
                (put! ch [nil]))
            (do (js/console.error "Failed to save sheet")
                (put! ch [(js/Error. "Failed to save sheet")])))))

      ; return the channel
      ch)))

(defn create-provider []
  (->GDriveProvider))
