(ns ^{:author "Daniel Leong"
      :doc "Google Drive powered Provider"}
  wish.providers.gdrive
  (:require-macros [cljs.core.async :refer [go]]
                   [wish.util.async :refer [call-with-cb->chan]]
                   [wish.util.log :as log :refer [log]])
  (:require [clojure.core.async :refer [chan put! <! >!]]
            [clojure.string :as str]
            [wish.config :refer [gdrive-client-id]]
            [wish.providers.core :refer [IProvider load-raw]]
            [wish.providers.gdrive.api :as api :refer [->clj]]
            [wish.sheets.util :refer [make-id]]
            [wish.util :refer [>evt]]))


;;
;; Constants
;;

;; Array of API discovery doc URLs for APIs used by the quickstart
(def ^:private discovery-docs #js ["https://www.googleapis.com/discovery/v1/apis/drive/v3/rest"])

;; Authorization scopes required by the API; multiple scopes can be
;; included, separated by spaces.
(def ^:private scopes (str/join
                        " "
                        ["https://www.googleapis.com/auth/drive.file"]))

(def ^:private sheet-desc "WISH Character Sheet")
(def ^:private sheet-mime "application/edn")
(def ^:private sheet-props {:wish-type "wish-sheet"})
(def ^:private source-desc "WISH Data Source")
(def ^:private source-mime "application/edn")
(def ^:private source-props {:wish-type "data-source"})

;;
;; Internal util
;;

(defn- refresh-auth []
  (call-with-cb->chan
    (js/gapi.auth.authorize
      #js {:client_id gdrive-client-id
           :scope scopes
           :immediate true})))

(defn- do-with-retry
  [f & args]
  (go (let [[error resp] (<! (apply f args))]
        (cond
          (and error
               (= 401 (.-code error)))
          ; refresh creds and retry
          (let [_ (log/info "Refreshing auth before retrying " f "...")
                [refresh-err refresh-resp] (<! (refresh-auth))]
            (if refresh-err
              (do
                (log/warn "Auth refresh failed:" refresh-resp)
                [refresh-err nil])

              (let [_ (log/info "Auth refreshed! Retrying...")
                    [retry-err retry-resp] (<! (apply f args))]
                (if retry-err
                  (do
                    (log/err "Even after auth refresh, " f " failed: " resp)
                    [retry-err nil])

                  ; upload retry succeeded!
                  [nil retry-resp]))))

          ; unexpected error:
          error (do
                  (log/err f " ERROR:" error)
                  [error nil])

          ; no problem; pass it along
          :else [nil resp]))))

(defn- reliably
  "Given a function `f`, return a new function that
   applies its arguments to `f`, and auto-retries on
   auth failure"
  [f]
  (partial do-with-retry f))

;;
;; gapi wrappers
;;

(def ^:private get-file (reliably api/get-file))
(def ^:private get-meta (reliably api/get-meta))
(def ^:private query-files (reliably api/query-files))
(def ^:private upload-data (reliably api/upload-data))

(defn- ensure-meta
  ([file-id metadata]
   (ensure-meta file-id metadata false))
  ([file-id metadata force?]
   (go (if force?
         (<! (api/update-meta file-id metadata))

         (let [[err resp] (<! (get-meta file-id))]
           (when (or err
                     (not= (select-keys
                             resp
                             (keys metadata))
                           metadata))
             (log "Updating " file-id "metadata <- " metadata)
             (api/update-meta file-id metadata)))))))

(defn- do-query-files
  "Convenience wrapper around query-files that provides a
   callback-style interface"
  [q & {:keys [on-error on-success]
        :or {on-error (fn [e]
                        (log/err "ERROR listing files" e))}
        :as opts}]
  (go (let [[err resp] (<! (apply query-files
                                  q
                                  (dissoc opts :on-error :on-success)))]
        (if err
          (on-error err)
          (on-success resp)))))

;;
;; State management and API interactions
;;

; gapi availability channel. Once js/gapi is
; available, this atom is reset! to nil. Due to
; how the (go) macro works, we can't have a nice
; convenience function to use this, so callers will
; have to look like:
;
;   (when-let [ch @gapi-available?]
;     (<! ch))
;
(defonce ^:private gapi-available? (atom (chan)))

(defn- set-gapi-available! []
  (when-let [gapi-available-ch @gapi-available?]
    (reset! gapi-available? nil)
    (put! gapi-available-ch true)))

(defn- auth-instance
  "Convenience to get the gapi auth instance:
   gapi.auth2.getAuthInstance().
   @return {gapi.AuthInstance}"
  []
  (js/gapi.auth2.getAuthInstance))

(defn- current-user []
  (some-> (auth-instance)
          (.-currentUser)
          (.get)))

(defn- access-token
  "When logged in, get the current user's access token"
  []
  (-> (current-user)
      (.getAuthResponse)
      (.-access_token)))

(defn- update-signin-status!
  [signed-in?]
  (log/info "signed-in? <-" signed-in?)
  (>evt [:put-provider-state! :gdrive (if signed-in?
                                        :signed-in
                                        :signed-out)])
  (when signed-in?
    (>evt [:mark-provider-listing! :gdrive true])
    (do-query-files
      "appProperties has { key='wish-type' and value='wish-sheet' }"
      :on-success (fn on-files-list [files]
                    (log/info "Found: " files)
                    (>evt [:add-sheets files])
                    (>evt [:mark-provider-listing! :gdrive false])))))

(defn- on-client-init
  []
  (log "gapi client init!")
  (set-gapi-available!)

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
             :clientId gdrive-client-id
             :scope scopes})
      (.then on-client-init)))

;;
;; NOTE: Exposed to index.html
(defn ^:export handle-client-load []
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

(defn active-user []
  (when-let [profile (some-> (current-user)
                             (.getBasicProfile))]
    {:name (.getName profile)
     :email (.getEmail profile)}))


; ======= file picker ======================================

(defonce ^:private picker-api-loaded (atom false))

(defn- do-pick-file
  [{:keys [mimeType description props]}]
  ; NOTE: I don't love using camel case in my clojure code,
  ; but since we're using it everywhere else for easier compat
  ; with google docs, let's just use it here for consistency.

  (let [ch (chan)]
    (-> (js/google.picker.PickerBuilder.)
        (.addView (doto (js/google.picker.View.
                          js/google.picker.ViewId.DOCS)
                    (.setMimeTypes "application/edn,application/json,text/plain")))
        (.addView (js/google.picker.DocsUploadView.))
        (.setAppId gdrive-client-id)
        (.setOAuthToken (access-token))
        (.setCallback
          (fn [result]
            (let [result (->clj result)
                  uploaded? (= "upload"
                               (-> result :viewToken first))]
              (log "Picked: (wasUpload=" uploaded? ") " result)
              (case (:action result)
                "cancel" (put! ch [nil nil])
                "picked" (let [file (-> result
                                        :docs
                                        first
                                        (select-keys [:id :name]))
                               file-id (:id file)
                               wish-file (update file :id
                                                 (partial make-id :gdrive))]

                           ; update mime type and annotate with :wish-type
                           (ensure-meta
                             file-id
                             {:appProperties props
                              :description description}

                             ; force update when uploaded (skip a roundtrip)
                             uploaded?)

                           (put! ch [nil wish-file]))
                (log "Other pick action")))))
        (.build)
        (.setVisible true))
    ; return the channel
    ch))

(defn pick-file [opts]
  (if @picker-api-loaded
    ; loaded! do it now
    (do-pick-file opts)

    ; load first
    (let [ch (chan)]
      (log "Loading picker API")
      (js/gapi.load "picker"
                    (fn []
                      (log "Loaded picker! Waiting for result")
                      (reset! picker-api-loaded true)
                      (go (>!
                            ch
                            (<! (do-pick-file opts))))))
      ch)))

; ======= Provider def =====================================

(deftype GDriveProvider []
  IProvider
  (id [this] :gdrive)

  (create-sheet [this file-name data]
    (log/info "Create sheet " file-name)
    (go (let [[err resp] (<! (upload-data
                               :create
                               {:name file-name
                                :description sheet-desc
                                :mimeType sheet-mime
                                :appProperties sheet-props}
                               (str data)))]
          (if err
            [err nil]

            (let [pro-sheet-id (:id resp)]
              (log/info "CREATED" resp)
              [nil (make-id :gdrive pro-sheet-id)])))))

  #_(delete-sheet [this info]
      (log/info "Delete " (:gapi-id info))
      (-> js/gapi.client.drive.files
          (.delete #js {:fileId (:gapi-id info)})
          (.then (fn [resp]
                   (log/info "Deleted!" (:gapi-id info)))
                 (fn [e]
                   (log/warn "Failed to delete " (:gapi-id info))))))

  (init! [this]) ; nop

  (load-raw
    [this id]
    (go (let [_ (when-let [ch @gapi-available?]
                  (<! ch))

              [err resp :as r] (<! (get-file id))]
          (if err
            (let [status (.-status err)]
              (log/err "ERROR loading " id err)
              (if (= 404 status)
                ; possibly caused by permissions
                [(ex-info
                   (ex-message err)
                   {:permissions? true
                    :provider :gdrive
                    :id id}
                   err)
                 nil]

                ; some other error
                [err nil]))

            ; success; return unchanged
            r))))

  (query-data-sources [this]
    ; TODO indicate query state?
    (do-query-files
      "appProperties has { key='wish-type' and value='data-source' }"
      :on-success (fn [files]
                    (log "Found data sources: " files)
                    (>evt [:add-data-sources
                           (map (fn [[id file]]
                                  (assoc file :id id))
                                files)]))))

  (register-data-source [this]
    ; TODO sanity checks galore
    (pick-file {:mimeType source-mime
                :description source-desc
                :props source-props}))

  (save-sheet [this file-id data]
    (log/info "Save " file-id data)
    (upload-data
      :update
      {:fileId file-id
       :mimeType sheet-mime
       :description sheet-desc
       :name (:name data)}
      (str data))))

(defn create-provider []
  (->GDriveProvider))
