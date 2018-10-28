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
            [wish.util.http :refer [POST]]))

(def push-server-version "v1")

(def ^:private push-url-base (str config/push-server "/" push-server-version "/push"))

(def ^:private reload-changed-throttle-ms 750)

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


; ======= push event handling =============================

(defmulti on-push! (comp keyword :event))
(defmethod on-push! :need-watch
  [{{:keys [id]} :data}]
  (let [id (keyword id)]
    (log/todo "Create watch for sheet " (keyword id))))

;; "changed" handling

(defonce ^:private pending-changes (atom {:timer nil
                                          :ids #{}}))

(defn- reload-changed []
  (swap! pending-changes
         (fn [{:keys [ids]}]
           (>evt [:reload-changed! ids])

           ; reset state:
           {:timer nil :ids #{}})))

(defmethod on-push! :changed
  [{{:keys [id]} :data}]
  (let [changed-id (keyword id)]
    (log "Sheet changed" changed-id)
    (swap! pending-changes
           (fn [{:keys [timer ids] :as old}]
             (if (contains? ids changed-id)
               ; ignore the dup notification
               old

               ; cancel any old timer and start over
               (do
                 (when timer
                   (js/clearTimeout timer))

                 {:ids (conj ids changed-id)
                  :timer (js/setTimeout
                           reload-changed
                           reload-changed-throttle-ms)}))))))

;; fallback

(defmethod on-push! :default
  [evt]
  (log/warn "Unknown push event: " evt))


; ======= connect to a created session ====================
; NOTE event handler fns declared separately to improve hot-reload developer UX

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

(defn- on-message [evt]
  (let [data (-> (.-data evt)
                 (js/JSON.parse)
                 (js->clj :keywordize-keys true))]
    (log/info "Received: " data)
    (on-push! data)))

(defn- on-open [session-id]
  (log/info "Connected to session " session-id))

(defn connect [session-id]
  (log "Connecting to session " session-id)
  (doto (js/EventSource.
          (str push-url-base "/sessions/" session-id))
    (.addEventListener "error" on-error)
    (.addEventListener "open" (fn []
                                (on-open session-id)))
    (.addEventListener "message" on-message)))
