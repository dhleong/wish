(ns ^{:author "Daniel Leong"
      :doc "core.async http wrapper"}
  wish.util.http
  (:require [clojure.core.async :refer [chan put!]]
            [ajax.core :as ajax]))

(defn GET
  ([url]
   (GET url nil))

  ([url {:keys [response-format]}]
   (let [ch (chan 1)]
     (ajax/GET url
               {:handler (fn [raw]
                           (put! ch [nil raw]))
                :response-format (or response-format :json)
                :keywords? true
                :error-handler (fn [e]
                                 (put! ch [e]))})
     ch)))

(defn POST
  ([url body]
   (POST url body nil))

  ([url body {:keys [response-format]}]
   (let [ch (chan 1)]
     (ajax/POST url
                {:handler (fn [raw]
                            (put! ch [nil raw]))
                 :response-format (or response-format :json)
                 :keywords? true
                 :params body
                 :format :json
                 :error-handler (fn [e]
                                  (put! ch [e]))})
     ch)))
