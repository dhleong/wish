(ns ^{:author "Daniel Leong"
      :doc "Debug config"}
  wish.config)

(goog-define ^string VERSION "DEV-SNAPSHOT")

(def debug?
  ^boolean goog.DEBUG)

(def server-root "")

(def gdrive-client-id "661182319990-1uerkr0pue6k60a83atj2f58md95fb1b.apps.googleusercontent.com")

(def push-server "http://localhost:4321")
