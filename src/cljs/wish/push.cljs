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
            (log/warn "Unable to create push session" err)
            (>evt [::session-created interested-ids id]))))))


; ======= push event handling =============================

(defmulti on-push! (comp keyword :event))
(defmethod on-push! :need-watch
  [{{:keys [id]} :data}]
  (let [id (keyword id)]
    (log/todo "Create watch for sheet " (keyword id))))

(defmethod on-push! :changed
  [{{:keys [id]} :data}]
  (let [id (keyword id)]
    (log/todo "Sheet changed" (keyword id))))

(defmethod on-push! :default
  [evt]
  (log/warn "Unknown push event: " evt))


; ======= connect to a created session ====================

; NOTE event handler fns declared separately to improve hot-reload developer UX
(defn- on-error [e]
  (log/warn "Error with push session" e))

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
