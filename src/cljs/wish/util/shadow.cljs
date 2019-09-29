(ns wish.util.shadow
  "Auto-reconnect for shadow-cljs; ONLY to be used in debug mode"
  (:require-macros [wish.util.log :refer [log]])
  (:require [clojure.string :as str]
            #_[shadow.cljs.devtools.client.browser :as shadow]
            #_[shadow.cljs.devtools.client.hud :as shadow-hud]))

(defonce ^:private reconnect-interval (volatile! nil))

(defn- start-reconnect! []
  (when-let [old @reconnect-interval]
    (js/clearTimeout old))

  (log "prepare auto reconnect!")
  (vreset! reconnect-interval
           (js/setTimeout
             (fn []
               (if @js/shadow.cljs.devtools.client.browser.socket_ref
                 (do
                   (log "reconnected!")
                   (js/clearTimeout @reconnect-interval))
                 (do
                   (log "reconnecting...")
                   (js/shadow.cljs.devtools.client.browser.ws_connect))))
             500)))

(defn- find-connection-error-node [mutations]
  (->> mutations
       (map (fn [m]
              (let [added (.-addedNodes m)]
                (->> (range 0 (.-length added))
                     (keep (fn [i]
                             (let [n (aget added i)]
                               (when (= "shadow-connection-error"
                                        (.-id n))
                                 n))))
                     first))))
       first))

(defn- check-for-shadow-disconnect [mutations]
  (when-let [n (find-connection-error-node mutations)]
    (let [text (.-innerText n)]
      (when (str/ends-with? text "Connection closed!")
        (start-reconnect!)))))

(defonce ^:private observer
  (delay
    (js/MutationObserver.
      (fn [mutations]
        (check-for-shadow-disconnect mutations)))))

(defn listen! []
  (.observe @observer
            js/document.body
            #js {:childList true}))
