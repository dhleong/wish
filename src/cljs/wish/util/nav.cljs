(ns ^{:author "Daniel Leong"
      :doc "Navigation util"}
  wish.util.nav
  (:require [clojure.string :as string]
            [reagent.dom :as reagent-dom]
            [re-frame.core :refer [dispatch-sync]]
            [secretary.core :as secretary]
            [goog.events :as gevents]
            [goog.history.EventType :as HistoryEventType]
            [pushy.core :as pushy]
            [wish.util :refer [is-ios? >evt]])
  (:import goog.History))

(goog-define ^boolean LOCAL false)

; NOTE: figwheel css live-reload doesn't work so well with
; the fancy nav
(def ^:private pushy-supported? (and (not LOCAL)
                                     (pushy/supported?)))

(def ^:private pushy-prefix "/wish")

(defn init! []
  (secretary/set-config! :prefix (if pushy-supported?
                                   (str pushy-prefix "/")
                                   "#")))

;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (if pushy-supported?
    ; fancy html5 navigation
    (let [history (pushy/pushy
                    secretary/dispatch!
                    (fn [x]
                      (when (secretary/locate-route x)
                        x)))]
      (pushy/start! history))

    ; #-based navigation
    (doto (History.)
      (gevents/listen
        HistoryEventType/NAVIGATE
        (fn [event]
          (secretary/dispatch! (.-token event))))
      (.setEnabled true))))

(defn prefix
  "Prefix a link as necessary for :href-based navigation to work"
  [raw-link]
  (if pushy-supported?
    (str pushy-prefix raw-link)
    (str "#" raw-link)))

(defn navigate!
  [& args]
  (let [evt (into [:navigate!] args)]
    (if (is-ios?)
      ; NOTE: on iOS we do some whacky shit to prevent awful flashes
      ;  when swiping back. hopefully there's a more efficient way
      ;  to do this, but for now... this works
      (do
        (dispatch-sync evt)
        (reagent-dom/force-update-all))

      ; When we don't have to worry about back-swipe, we can be more
      ;  relaxed about handling navigation
      (>evt evt))))

(defn replace!
  "Wrapper around js/window.location.replace"
  [new-location]
  (js/window.location.replace
    (prefix new-location)))

(defn sheet-url
  "Generate the url to a sheet, optionally with
   extra path sections after it"
  [id & extra-sections]
  (apply str "/sheets/" (namespace id)
       "/" (name id)
       (when extra-sections
         (interleave (repeat "/")
                     (map
                       #(if (keyword? %)
                          (name %)
                          (str %))
                       extra-sections)))))
