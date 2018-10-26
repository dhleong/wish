(ns ^{:author "Daniel Leong"
      :doc "Google Drive powered Provider"}
  wish.providers.gdrive
  (:require-macros [cljs.core.async :refer [go go-loop]]
                   [wish.util.async :refer [call-with-cb->chan]]
                   [wish.util.log :as log :refer [log]])
  (:require [clojure.core.async :refer [promise-chan close! put! to-chan <!]]
            [clojure.string :as str]
            [goog.dom :as dom]
            [wish.config :refer [gdrive-client-id]]
            [wish.data :as data]
            [wish.providers.core :refer [IProvider load-raw]]
            [wish.providers.gdrive.api :as api :refer [->clj]]
            [wish.sheets.util :refer [make-id]]
            [wish.util :refer [>evt]]
            [wish.util.async :refer [promise->chan]]))


;;
;; Constants
;;

;; Array of API discovery doc URLs for APIs used by the quickstart
(def ^:private discovery-docs #js ["https://www.googleapis.com/discovery/v1/apis/drive/v3/rest"])

(def ^:private drive-read-scope
  "https://www.googleapis.com/auth/drive.readonly")
(def ^:private drive-full-scope
  "https://www.googleapis.com/auth/drive")

;; Authorization scopes required by the API; multiple scopes can be
;; included, separated by spaces.
(def ^:private scopes (str/join
                        " "
                        ["https://www.googleapis.com/auth/drive.file"
                         "https://www.googleapis.com/auth/drive.install"]))

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

          ; network error
          (and error
               (str/includes?
                 (or (some-> error
                             (.-result)
                             (.-error)
                             (.-message))
                     (some-> error
                             (.-message))
                     (ex-message error)
                     (log/warn "No message on " error))
                 "network"))
          [(ex-info
             "A network error occured"
             {:network? true}
             error)
           nil]

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

; gapi availability channel. it starts out as a channel,
; so function calls depending on gapi being available can
; wait on it to discover the state (see `when-gapi-available`).
; Once gapi availability is determined, this atom is reset!
; to one of the valid provider states (:ready, :unavailable, :signed-out)
(defonce ^:private gapi-state (atom (promise-chan)))

(defn- set-gapi-state! [new-state]
  (swap! gapi-state
         (fn [old-state]
           (if (keyword? old-state)
             ; if it's not a channel then this is probably not
             ; part of init!, instead from config. Dispatch the event
             (>evt [:put-provider-state! :gdrive new-state])

             ; if it is a channel, however, write to it!
             (do (put! old-state new-state)
                 (close! old-state)))

           new-state)))

(defn- when-gapi-available
  "Apply `args` to `f` when gapi is available, or (if it's
   unavailable) return an appropriate error"
  [f & args]
  (go (let [availability @gapi-state]
        (log "when-gapi-available: " availability f args)
        (cond
          (= :unavailable availability)
          [(ex-info
             "GAPI unavailable"
             {:network? true})
           nil]

          ; wait on the channel, if there is one
          (not (keyword? availability))
          (let [from-ch (<! availability)]
            (if (= :ready from-ch)
              ; ready
              (do
                (log "got availability; then: " f args)
                (<! (apply f args)))

              ; try again
              (do
                (log "Not ready: " from-ch)
                [(js/Error. (str "Error? " from-ch))])))

          ; should be available! go ahead and load
          :else
          (<! (apply f args))))))

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

(defn- auth-response []
  (some-> (current-user)
          (.getAuthResponse)))

(defn- access-token
  "When logged in, get the current user's access token"
  []
  (some-> (auth-response)
          (.-access_token)))

(defn- update-signin-status!
  [signed-in?]
  (log/info "signed-in? <-" signed-in?)
  (set-gapi-state! (if signed-in?
                     :ready
                     :signed-out)))

(defn- on-client-init []
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

(defn- on-client-init-error [e]
  (log/warn "gapi client failed" e (js/JSON.stringify e))
  (set-gapi-state! :unavailable)

  ; TODO can we retry when network returns?
  )

(defn init-client! []
  (log "init-client!")
  (-> (js/gapi.client.init
        #js {:discoveryDocs discovery-docs
             :clientId gdrive-client-id
             :scope scopes})
      (.then on-client-init
             on-client-init-error)))

(defn request-read!
  "Starts the flow to request readonly scope. Returns a channel"
  []
  (some-> (current-user)
          (.grant #js {:scope drive-read-scope})
          (promise->chan)))

(defn has-global-read?
  "Returns truthy if the active user should have read access
   to any file shared with them, else nil"
  []
  (when-let [user (current-user)]
    (or (.hasGrantedScopes user drive-read-scope)
        (.hasGrantedScopes user drive-full-scope))))

;;
;; NOTE: Exposed to index.html
(defn ^:export handle-client-load [success?]
  (log "load")
  (if success?
    (js/gapi.load "client:auth2",
                  #js {:callback init-client!
                       :onerror on-client-init-error})
    (on-client-init-error nil)))

(defn- retry-gapi-load! []
  ; NOTE we have to do a get off window, else cljs throws
  ; a reference error
  (if js/window.gapi
    ; we have gapi, but I guess one of the libs failed to load?
    (handle-client-load true)

    ; no gapi; add a new copy of the script node
    (do
      (log "Add a new gapi <script> node")
      (dom/appendChild
        (aget (dom/getElementsByTagName "head") 0)
        (dom/createDom dom/TagName.SCRIPT
                       #js {:onload (partial handle-client-load true)
                            :onerror (partial handle-client-load false)
                            :async true
                            :src "https://apis.google.com/js/api.js"
                            :type "text/javascript"})))))

;;
;; Public API
;;

(defn signin! []
  (-> (auth-instance)
      (.signIn)))

(defn signout! []
  (doto (auth-instance)
    (.disconnect)
    (.signOut)))

(defn active-user []
  (when-let [profile (some-> (current-user)
                             (.getBasicProfile))]
    {:name (.getName profile)
     :email (.getEmail profile)}))


; ======= file picker ======================================

(defn- do-pick-file
  [{:keys [mimeType description props]}]
  ; NOTE: I don't love using camel case in my clojure code,
  ; but since we're using it everywhere else for easier compat
  ; with google docs, let's just use it here for consistency.

  (let [ch (promise-chan)]
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

(def pick-file (api/when-loaded "picker" do-pick-file))


; ======= Share dialog ====================================

(defn- do-share-file [id]
  (doto (js/gapi.drive.share.ShareClient.)
    (.setOAuthToken (access-token))
    (.setItemIds #js [id])
    (.showSettingsDialog))

  ; make sure we return nil
  nil)

(def share! (api/when-loaded "drive-share" do-share-file))


; ======= file loading ====================================

(defn- do-load-raw [id]
  (go (let [[err resp :as r] (<! (get-file id))]
        (if err
          (let [status (.-status err)]
            (log/err "ERROR loading " id err)
            (cond
              ; possibly caused by permissions
              (= 404 status)
              [(ex-info
                 (ex-message err)
                 {:permissions? true
                  :provider :gdrive
                  :id id}
                 err)
               nil]

              ; signed out
              (= 403 status)
              [(ex-info
                 (ex-message err)
                 {:state :signed-out
                  :provider :gdrive
                  :id id}
                 err)
               nil]

              ; some other error
              :else
              [err nil]))

          ; success; return unchanged
          r))))

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

  (init! [this]
    (go (let [state @gapi-state]
          (cond
            ; try to load gapi again
            (= :unavailable state)
            (let [ch (promise-chan)]
              (log "reloading gapi")
              (reset! gapi-state ch)

              ; init a new load onto this promise-chan,
              ; and wait for the result
              (retry-gapi-load!)
              (<! ch))

            ; state is resolved; return directly
            (keyword? state)
            state

            ; wait on the channel for the state
            :else
            (<! state)))))

  (load-raw
    [this id]
    (when-gapi-available do-load-raw id))

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

  (query-sheets [this]
    (when-gapi-available
      query-files
      "appProperties has { key='wish-type' and value='wish-sheet' }"))

  (register-data-source [this]
    ; TODO sanity checks galore
    (pick-file {:mimeType source-mime
                :description source-desc
                :props source-props}))

  (save-sheet [this file-id data data-str]
    (if (= :ready @gapi-state)
      (do
        (log/info "Save " file-id data)
        (upload-data
          :update
          (cond-> {:fileId file-id
                   :mimeType sheet-mime
                   :description sheet-desc}
            ; update the :name if we can
            data (assoc :name (when-let [n (:name data)]
                                (str n "." data/sheet-extension))))
          data-str))

      ; not ready? don't try
      (to-chan [[(js/Error. "No network; unable to save sheet") nil]])))

  (watch-auth [this]
    (when-let [resp (auth-response)]
      {:id_token (.-id_token resp)
       :access_token (access-token)})))

(defn create-provider []
  (->GDriveProvider))
