(ns ^{:author "Daniel Leong"
      :doc "Google Drive API wrappers"}
  wish.providers.gdrive.api
  (:require-macros [wish.util.async :refer [call-with-cb->chan]]
                   [wish.util.log :as log :refer [log]])
  (:require [clojure.core.async :refer [chan]]
            [wish.sheets.util :refer [make-id]]
            [wish.util.async :refer [promise->chan]]))

(defn ->clj [v]
  (js->clj v :keywordize-keys true))

(defn query-files
  "List files matching the given query `q`. Accepts optional
   :max keyword arg indicating the most that will be returned
   (defaults to 50). Returns the usual style of channel."
  [q & {page-size :max
        :or {page-size 50}}]
  (letfn [(clean-resp [response]
            (log "FILES LIST:" response)
            (->> response
                 ->clj
                 :result
                 :files
                 (map
                   (fn [raw-file]
                     [(make-id :gdrive (:id raw-file))
                      (select-keys raw-file
                                   [:name])]))))]

    (-> js/gapi.client.drive.files
      (.list #js {:q q
                  :pageSize page-size
                  :spaces "drive,appDataFolder"
                  :fields "nextPageToken, files(id, name)"})
      (promise->chan 1 (map (fn [[err resp :as r]]
                              (if resp
                                ; success!
                                [err (clean-resp resp)]

                                ; err; return as-is
                                r)))))))

(defn get-file
  "Fetch the metadata of a file or, alternately, download an `alt`,
   if provided. Returns the usual style of channel."
  ([file-id]
   (get-file file-id nil))
  ([file-id alt]
   (-> js/gapi.client.drive.files
       (.get (if alt
               #js {:fileId file-id
                    :alt alt}
               #js {:fileId file-id
                    :fields "id, name, mimeType, description, appProperties"}))
       (promise->chan 1 (map (fn [[err resp :as r]]
                               (if (and resp
                                        (not alt))
                                 ; if we have a response, and we wanted the file
                                 ; metadata; clojure-ify it
                                 [err (->> resp
                                           ->clj
                                           :result)]

                                 ; just return as-is
                                 r)))))))

(defn update-meta
  "Update the metadata on a file. Useful for eg:
   (update-meta
     :new-source
     {:appProperties {:wish-type \"data-source\"}})"
  [file-id metadata]
  (-> js/gapi.client.drive.files
      (.update (clj->js
                 (assoc metadata
                        :fileId file-id)))
      promise->chan))

(defn upload-data
  "The GAPI client doesn't provide proper support for
   file uploads out-of-the-box, so let's roll our own.

   Returns a channel that emits [err, res], where err
   is non-nil on error, and res is non-nil on success."
  [upload-type metadata content]
  {:pre [(contains? #{:create :update} upload-type)
         (string? (:mimeType metadata))
         (not (nil? content))]}
  (let [base (case upload-type
               :create {:path "/upload/drive/v3/files"
                        :method "POST"}
               :update {:path (str "/upload/drive/v3/files/"
                                   (:fileId metadata))
                        :method "PATCH"})
        boundary "-------314159265358979323846"
        delimiter (str "\r\n--" boundary "\r\n")
        close-delim (str "\r\n--" boundary "--")
        body (str delimiter
                  "Content-Type: application/json\r\n\r\n"
                  (js/JSON.stringify (clj->js metadata))
                  delimiter
                  "Content-Type: " (:mimeType metadata) "\r\n\r\n"
                  content
                  close-delim)
        request (assoc base
                       :params {:uploadType "multipart"}
                       :headers
                       {:Content-Type
                        (str "multipart/related; boundary=\"" boundary "\"")}
                       :body body)]
    (call-with-cb->chan
      (.execute
        (js/gapi.client.request
          (clj->js request))))))
