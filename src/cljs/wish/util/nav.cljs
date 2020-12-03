(ns ^{:author "Daniel Leong"
      :doc "Navigation util"}
  wish.util.nav
  (:require [clojure.string :as str]
            [secretary.core :as secretary]
            [goog.events :as gevents]
            [goog.history.EventType :as HistoryEventType]
            [pushy.core :as pushy]
            [wish.config :as config]
            [wish.util :refer [>evt]])
  (:import goog.History))

(goog-define ^boolean LOCAL false)

; NOTE: figwheel css live-reload doesn't work so well with
; the fancy nav
(def ^:private pushy-supported? (and (not LOCAL)
                                     (pushy/supported?)))

(def ^:private pushy-prefix config/server-root)
(def ^:private secretary-prefix (if pushy-supported?
                                  (str pushy-prefix "/")
                                  "#"))

(defn init! []
  (secretary/set-config! :prefix secretary-prefix))

;; from secretary
(defn- uri-without-prefix [uri]
  (str/replace uri (re-pattern (str "^" secretary-prefix)) ""))
(defn- uri-with-leading-slash
  "Ensures that the uri has a leading slash"
  [uri]
  (if (= "/" (first uri))
    uri
    (str "/" uri)))

;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (if pushy-supported?
    ; fancy html5 navigation
    (let [history (pushy/pushy
                    secretary/dispatch!
                    (fn [x]
                      (let [[uri-path _query-string]
                            (str/split (uri-without-prefix x) #"\?")
                            uri-path (uri-with-leading-slash uri-path)]
                        (when (secretary/locate-route uri-path)
                          x))))]
      (pushy/start! history))

    ; #-based navigation
    (doto (History.)
      (gevents/listen
        HistoryEventType/NAVIGATE
        (fn [^js event]
          (secretary/dispatch! (.-token event))))
      (.setEnabled true))))

(defn prefix
  "Prefix a link as necessary for :href-based navigation to work"
  [raw-link]
  (if pushy-supported?
    (str pushy-prefix raw-link)
    (str "#" raw-link)))

(defn navigate! [& args]
  (>evt (into [:navigate!] args)))

(defn replace!
  "Wrapper around js/window.location.replace"
  [new-location]
  (js/window.location.replace
    (prefix new-location)))


(defn- base-sheet-url
  "Generate the url to a sheet, optionally with
   extra path sections after it"
  [kind id & extra-sections]
  (apply str "/" kind
         "/" (namespace id)
         "/" (name id)
         (when extra-sections
           (interleave (repeat "/")
                       (map
                         #(if (keyword? %)
                            (name %)
                            (str %))
                         extra-sections)))))

(defn campaign-url
  "Generate the url to a campaign, optionally with
   extra path sections after it"
  [id & extra-sections]
  (apply base-sheet-url "campaigns" id extra-sections))

(defn campaign-invite-url
  "Generate the url to join a campaign"
  ([campaign-id invited-sheet-url]
   (campaign-invite-url campaign-id invited-sheet-url nil))
  ([campaign-id invited-sheet-url campaign-name]
   (str
     config/full-url-root
     (prefix
       (base-sheet-url "join-campaign" campaign-id
                       "n" (js/encodeURIComponent campaign-name)
                       "as"
                       (namespace invited-sheet-url) invited-sheet-url)))))

(defn sheet-url
  "Generate the url to a sheet, optionally with
   extra path sections after it"
  [id & extra-sections]
  (apply base-sheet-url "sheets" id extra-sections))


; ======= support back button to close overlays ===========

(def ^:private overlay-suffix
  (if pushy-supported?
    "#overlay"
    "?overlay"))

(defn- dismiss-from-event []
  (set! js/window.onpopstate nil)
  (>evt [:toggle-overlay nil]))

(defn make-overlay-closeable!
  [closable?]
  (cond
    closable?
    (do
      ; such hacks: navigate to the same url but with a trivial suffix
      ; appended (so it doesn't register as a different page) then
      ; hook onpopstate
      (set! js/window.location (str js/window.location overlay-suffix))
      (set! js/window.onpopstate dismiss-from-event))

    ; closing manually; if we have the ?overlay url, go back to remove it
    (str/ends-with? (str js/window.location) overlay-suffix)
    (do
      ; stop listening to onpopstate
      (set! js/window.onpopstate nil)
      (js/history.go -1))))
