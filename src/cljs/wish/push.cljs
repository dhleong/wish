(ns ^{:author "Daniel Leong"
      :doc "Push-notification API"}
  wish.push
  (:require-macros [cljs.core.async :refer [go]]
                   [wish.util.log :refer [log] :as log])
  (:require [clojure.core.async :refer [<!]]
            [wish.config :as config]
            [wish.providers :as providers]
            [wish.sheets.util :refer [unpack-id]]
            [wish.util :refer [>evt]]
            [wish.util.http :refer [POST]]
            [wish.util.throttled-set :refer [throttle-with-set]]))

(def push-server-version "v1")

(def ^:private push-url-base (str config/push-server "/" push-server-version "/push"))

(def ^:private reload-changed-throttle-ms 750)
(def ^:private create-watches-throttle-ms 3000)

; ======= session creation ================================

(defn session-args [auth interested-ids]
  {:auth auth

   :ids (->> interested-ids

             ; remove sheet ids for which we don't have auth
             (filter (fn [sheet-id]
                       (let [[provider-id _] (unpack-id sheet-id)]
                         (contains? auth provider-id))))

             ; default serialization of a keyword drops the namespace
             (map (fn [id]
                    (subs (str id) 1))))})

(defn- do-create-session [auth interested-ids]
  (POST (str push-url-base "/sessions")
        (session-args auth interested-ids)))

(defn create-session [interested-ids]
  (when-let [auth (providers/watch-auth-map interested-ids)]
    (go (when-let [[err {:keys [id]}] (<! (do-create-session auth interested-ids))]
          (if err
            (do (log/warn "Unable to create push session" err)
                (>evt [:push/retry-later]))

            (>evt [::session-created interested-ids id]))))))


; ======= watch creation ==================================

(defn- do-create-watches [session-id auth interested-ids]
  (POST (str push-url-base "/sessions/watch")
        (assoc (session-args auth interested-ids)
               :sessionId session-id)))

(defn create-watches [session-id ids]
  (log/info "Creating watches on " session-id " for " ids)
  (when-let [auth (providers/watch-auth-map ids)]
    (go (when-let [[err _] (<! (do-create-watches session-id auth ids))]
          (when err
            (log/warn "Failed to create watches for " ids ": " err))))))


; ======= push event handling =============================

(defmulti on-push! (fn [session-id evt] (-> evt :event keyword)))

;;
;; "need-watch" handling

(defonce ^:private create-watches-throttled
  (throttle-with-set
    create-watches-throttle-ms
    (fn [ids session-id]
      ; NOTE "extra" args (eg: session-id) are passed
      ; *after* the ids set (throttle-with-set semantics)
      ; but our fns all put session-id first for consistency
      (create-watches session-id ids))))

(defmethod on-push! :need-watch
  [session-id {{:keys [id]} :data}]
  (let [id (keyword id)]
    (log "Need watch for sheet " (keyword id)
         "for session " session-id)
    ; NOTE "extra" args must be passed *after* ids
    (create-watches-throttled id session-id)))

;;
;; "changed" handling

(defonce ^:private reload-changed
  (throttle-with-set
    reload-changed-throttle-ms
    (fn [changed-ids]
      (>evt [:reload-changed! changed-ids]))))

(defmethod on-push! :changed
  [session-id {{:keys [id]} :data}]
  (let [changed-id (keyword id)]
    (log "Sheet changed" changed-id)
    (reload-changed changed-id)))

;;
;; fallback

(defmethod on-push! :default
  [session-id evt]
  (log/warn "Unknown push event: " evt))


; ======= connect to a created session ====================
; NOTE event handler fns declared separately to improve hot-reload developer UX

;; EventSource:

(def ^:private connection-ready-states
  {0 :connecting
   1 :open
   2 :closed})

(defn ready-state [event-source]
  (if event-source
    (get connection-ready-states (.-readyState event-source))
    :closed))

(defn- on-error [evt]
  ; on fatal errors, we should try to create a new session after a delay
  (let [state (-> evt (.-target) ready-state)]
    ; NOTE: if we get an error when the state is :connecting, EventSource
    ; is handling it; we shouldn't get any in :open state
    (if-not (= :closed state)
      (log/info "Reconnecting to push session; state=" state)

      (do
        (log/info "Error with push session; state=" state)
        (>evt [:push/retry-later])))))

(defn- on-message [session-id evt]
  (let [data (-> (.-data evt)
                 (js/JSON.parse)
                 (js->clj :keywordize-keys true))]
    (log/info "Received: " data)
    (on-push! session-id data)))

(defn- on-open [session-id]
  (log/info "Connected to session " session-id))

(defn- connect-sse [session-id]
  (doto (js/EventSource.
          (str push-url-base "/sessions/" session-id))
    (.addEventListener "error" on-error)
    (.addEventListener "open" (fn []
                                (on-open session-id)))
    (.addEventListener "message" #(on-message session-id %))))

;; socket.io:

(defn- on-sio-error [e]
  (log/warn "SIO error" e))

(defn- on-sio-message [session-id m]
  (log/todo "SIO message: " session-id m))

(defn- connect-sio [session-id]
  (doto (js/io (str config/push-server "/" session-id)
               #js {:path (str "/" push-server-version
                               "/push/sessions/io/")})
    (.on "error" on-sio-error)
    (.on "connect" #(on-open session-id))
    (.on "message" #(on-sio-message session-id %))))

;;
;; Public interface
;;

(defn connect [session-id]
  (log "Connecting to session " session-id)
  ; NOTE: we currently *only* try to connect via socket.io,
  ; because now.sh doesn't handle EventSource properly....
  (connect-sio session-id))

(defn close [connection]
  ; should work for both EventSource and socket.io
  (.close connection))
