(ns wish.worker.core
  (:require-macros [wish.util.log :as log :refer [log]])
  (:require [clojure.string :as str]
            [cljs.reader :as edn]
            [cemerick.url :as url]
            [wish.config :as config]))

(def cache-name "wish")

; how long to wait before deciding the network is slow
; and loading assets from cache
(def default-network-timeout-ms 9500)

; we'll wait slightly longer for external assets
(def external-network-timeout-ms 15000)

(def shell-root (str config/server-root "/"))

(def files-to-cache #js [shell-root
                         (str shell-root "js/compiled/app.js")
                         (str shell-root "css/site.css")
                         (str shell-root "assets/icon/icon-192.png")

                         ; external resources:
                         "https://fonts.googleapis.com/icon?family=Material+Icons"])

(def push-server-url (url/url config/push-server))


; ======= utils ===========================================

(defn last-modified [resp]
  (some-> resp
          (.-headers)
          (.get "last-modified")))


; ======= cache manipulation ==============================

(defn purge-old-caches []
  (-> js/caches
      .keys
      (.then #(->> %
                   (keep (fn [cache-key]
                           (when-not (= cache-name cache-key)
                             (.delete js/caches cache-key))))
                   clj->js
                   js/Promise.all))
      (.then #(log "Purged old cache entries"))))


; ======= client messaging ================================

(defn post! [c message]
  (.postMessage c (str message)))

(defn post-message! [message]
  (let [message-str (str message)]
    (-> (js/self.clients.matchAll)
        (.then (fn [clients]
                 (doseq [c clients]
                   (.postMessage c message-str)))))))


; ======= resource fetching ===============================

(defn fetch-with-timeout
  ([req]
   (fetch-with-timeout req default-network-timeout-ms))
  ([req timeout-ms]
   (js/Promise.
     (fn [presolve preject]
       (let [abort-controller (js/AbortController.)
             timeout-timer (js/setTimeout
                             (fn []
                               (log/info "Abort slow request " req)
                               ; signal the request to stop
                               (.abort abort-controller)
                               (preject (js/Error.
                                          "Request failed (network delay / timed out)")))
                             timeout-ms)]
         (-> (js/fetch req
                       #js {:signal (.-signal abort-controller)

                            ; no-cache means always validate responses in in the
                            ; browser cache with eg: if-modified-since requests.
                            ; this ensures we always get the latest-deployed version
                            :cache "no-cache"
                            })
             (.then presolve preject)
             (.finally #(js/clearTimeout timeout-timer))))))))

(defn fetch-and-cache [req]
  (-> js/caches
      (.open cache-name)
      (.then (fn [cache]
               (-> (fetch-with-timeout req)
                   (.then (fn [resp]
                            ;; (log "Attempt to cache: " req " -> " resp)
                            (.put cache req (.clone resp))
                            resp)))))))

(defn fetch-with-cache [to-match]
  ; we always prefer network if we can
  (-> (fetch-and-cache to-match)
      (.catch (fn [e]
                (log "Unable to fetch " to-match "; checking cache. e=" e)
                (-> js/caches
                    (.match to-match)
                    (.then (fn [resp]
                             (or resp
                                 (do
                                   (log/warn "Couldn't fetch " to-match " from cache")
                                   (throw e))))))))))

(defn never-cache? [url]
  ; don't cache other schemes
  (or (not (contains? #{"http" "https"}
                      (:protocol url)))

      ; don't cache gapi; it's simpler to just use local-storage
      ; and handle caching sheets and data sources ourselves
      (contains? #{"apis.google.com"}
                 (:host url))

      ; don't cache shadow-cljs requests
      (and config/debug?
           (str/starts-with? (:path url) "/worker/"))

      ; also, don't interfere with requests to the push-server.
      ; This is generally only a problem for local dev
      (and (= (:port push-server-url)
              (:port url))
           (= (:host push-server-url)
              (:host url)))))

(defn wish-asset? [url]
  (contains? #{"localhost"
               "dhleong.github.io"}
             (:host url)))

(defn asset-file? [url]
  (let [path (:path url)]
    (or (str/ends-with? path ".css")
        (str/ends-with? path ".edn")
        (str/ends-with? path ".js")
        (str/ends-with? path ".json")
        (str/ends-with? path ".png")
        (str/ends-with? path ".html"))))

(defn shell-root?
  "The shell-root is any URL from which we want index.html"
  [url]
  (and (wish-asset? url)

       ; all shell URLs have an anchor locally
       (or (not (str/blank? (:anchor url)))

           ; files with these extensions are never the shell root
           (not (asset-file? url)))))

(defn shell-asset?
  "A shell asset is one of the core files involved in rendering
   the app, such as app.js, but not any of the source data files,
   for example."
  [url]
  (and (wish-asset? url)

       (let [path (:path url)]
         (or (str/ends-with? path ".css")
             (str/ends-with? path ".js")
             (str/ends-with? path ".png")))))

(def ^:private shell-last-modified-changed (atom nil))
(def ^:private notify-shell-updated-timer (atom nil))

(defn- notify-shell-updated [new-modified]
  ; throttle notification, since multiple assets could be updating
  ; and we don't want to notify until they're all ready
  (swap! notify-shell-updated-timer
         (fn [old-timer]
           (when old-timer
             (js/clearTimeout old-timer))

           ; return the new timer value:
           (js/setTimeout
             #(swap! shell-last-modified-changed
                     (fn [last-value]
                       (when-not (= last-value new-modified)
                         (post-message! [:shell-updated new-modified]))
                       new-modified))
             1500))))


(defn- fetch-shell-path [path]
  ; shell files are special. we want to load them from cache as
  ; fast as possible, but still check for updates in the background.
  ; If we detect an update, we then notify any clients
  (log "fetching shell for " path)
  (-> js/caches
      (.match path)
      (.then (fn [resp]
               (if resp
                 ; success!
                 (let [resp-clone (.clone resp)
                       old-modified (last-modified resp-clone)]
                   (log "shell path for " path " -> " resp-clone)

                   ; in the background, fetch and cache any updates
                   (-> (fetch-and-cache path)
                       (.then (fn [updated-resp]
                                (let [updated-clone (.clone updated-resp)
                                      new-modified (or (last-modified updated-clone)

                                                       ; local testing:
                                                       (when config/debug?
                                                         "new-modified"))]
                                  (when-not (= old-modified new-modified)
                                    (log "Updated " path ": " new-modified
                                         " (was: " old-modified ")")

                                    ; notify the client of shell changes, but
                                    ; not too noisily
                                    (notify-shell-updated new-modified)))))
                       (.catch #(log/info "Error updating " path ": " %)))

                   ; return the cached response immediately
                   resp)

                 (do
                   (log "No cache for " path)
                   (fetch-and-cache path)))))))

(defn fetch-shell-asset [{:keys [path query] :as url}]
  (if (and config/debug?
           (seq query))
    ; probably a figwheel update; ignore cache
    (fetch-and-cache url)

    ; normal case
    (do
      (log "fetching shell asset for " path)
      (let [css-dir-start (.indexOf path "/css/")]
        (if (not= -1 css-dir-start)
          (fetch-shell-path (str config/server-root
                                 (subs path css-dir-start)))

          (let [js-dir-start (.indexOf path "/js/")]
            (if (not= -1 js-dir-start)
              (fetch-shell-path (str config/server-root
                                     (subs path js-dir-start)))

              (fetch-shell-path path))))))))


; ======= event handlers ==================================

(defn fetch-event [ev]
  (let [request (.-request ev)
        url (-> request .-url url/url)]
    (cond
      (never-cache? url)
      (do
        (log "Never cache: " url)
        (-> (fetch-with-timeout request external-network-timeout-ms)
            (.catch (fn [e]
                      (log/warn "Unable to fetch " url ": " e)
                      (js/Response. nil #js {:status 503})))))

      (shell-root? url)
      (fetch-shell-path shell-root)

      (shell-asset? url)
      (fetch-shell-asset url)

      :else
      (fetch-with-cache request))))

(defn install-service-worker [ev]
  (log/info "Installing" ev)
  (-> js/caches
      (.open cache-name)
      (.then (fn [cache]
               (log "Caching app shell...")
               (.addAll cache files-to-cache)))
      (.then #(log "Successfully installed!"))))

(defn activate-service-worker [_ev]
  (log/info "Activated.")
  (purge-old-caches))


; ======= message receiver ================================

(defn receive! [client [msg-type & _args :as msg]]
  (log "SW.receive! " msg-type)
  (case msg-type
    :ready (when-let [last-modified @shell-last-modified-changed]
             ; the client is ready; if we've detected any updates,
             ; let them know
             (post! client [:shell-updated last-modified]))

    (log/warn "Unexpected message from client:" msg)))


; ======= Attach events ===================================

(.addEventListener js/self "install" #(.waitUntil % (install-service-worker %)))
(.addEventListener js/self "fetch" #(.respondWith % (fetch-event %)))
(.addEventListener js/self "activate" #(.waitUntil % (activate-service-worker %)))
(.addEventListener js/self "message" #(receive! (.-source %)
                                                (edn/read-string
                                                  (.-data %))))
