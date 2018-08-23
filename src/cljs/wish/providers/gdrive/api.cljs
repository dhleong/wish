(ns ^{:author "Daniel Leong"
      :doc "Google Drive API wrappers"}
  wish.providers.gdrive.api
  (:require-macros [wish.util.async :refer [call-with-cb->chan]]
                   [wish.util.log :as log :refer [log]])
  (:require [clojure.string :as str]
            [clojure.core.async :refer [chan]]
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
                      (-> raw-file
                          (select-keys [:name])
                          (assoc :mine? (:ownedByMe raw-file)))]))))]

    (-> js/gapi.client.drive.files
      (.list #js {:q q
                  :pageSize page-size
                  :spaces "drive"
                  :fields "nextPageToken, files(id, name, ownedByMe)"})
      (promise->chan 1 (map (fn [[err resp :as r]]
                              (if resp
                                ; success!
                                [err (clean-resp resp)]

                                ; err; return as-is
                                r)))))))

(defn get-meta
  "Fetch the metadata on a file.
   Returns the usual style of channel."
  [file-id]
  (-> js/gapi.client.drive.files
      (.get #js {:fileId file-id
                 :fields "id, name, mimeType, description, appProperties"})
      (promise->chan 1 (map (fn [[err resp :as r]]
                              (if resp
                                [err (->> resp
                                          ->clj
                                          :result)]

                                ; just return as-is
                                r))))) )

; NOTE: public for testing
(defn fix-unicode [s]
  ; due to a bug in how the JS client (and builtin browser
  ; base64 decode fn `btoa` work) work, the body string as-is
  ; can munge unicode characters such as the emdash.
  ; see: https://stackoverflow.com/questions/30106476/using-javascripts-atob-to-decode-base64-doesnt-properly-decode-utf-8-strings
  ; and: https://issuetracker.google.com/issues/36759232
  ; and: https://github.com/google/google-api-javascript-client/issues/221
  ; and: https://developer.mozilla.org/en-US/docs/Web/API/WindowBase64/Base64_encoding_and_decoding
  (->> (str/replace s "\\n" "\n")
       (map (fn [c]
              (let [hex (-> c
                            (.charCodeAt 0)
                            (.toString 16))]
                (if (<= (count hex) 2)
                  ; fix base64 decoding issue
                  (str "%"
                       (.slice
                         (str "00" hex)
                         -2))

                  ; don't break a successfully decoded character
                  ; for example, `•` gets decoded without problem,
                  ; but in a URI component it needs to be more than
                  ; one %## thing
                  (js/encodeURIComponent c)))))
       (str/join)
       js/decodeURIComponent))

(defn get-file
  "Download the contents of a file.
   Returns the usual style of channel."
  [file-id]
  (-> js/gapi.client.drive.files
      (.get #js {:fileId file-id
                 :alt "media"})
      (promise->chan 1 (map (fn [[err resp]]
                              (if err
                                [err nil]
                                [err (fix-unicode
                                       (.-body resp))]))))))

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

(defn view-file-link
  "Generates an URL that can be used to 'view' a file
   and request access to it"
  [id]
  (str "https://drive.google.com/file/d/"
       id
       "/view"))
