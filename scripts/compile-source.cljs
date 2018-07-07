#!/usr/bin/env bash
"exec" "plk" "-Sdeps" "{:deps {org.clojure/tools.cli {:mvn/version \"0.3.5\"}}}" "-sf" "$0" "$@"
(ns wish.builtin-source-compiler
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.reader.reader-types :refer [source-logging-push-back-reader]]
            [clojure.string :as str]
            [cljs.reader :as edn]
            [planck.core :refer [exit file-seq *err* slurp with-open]]
            [planck.environ :refer [env]]
            [planck.io :as io :refer [directory? file writer]]))

(defn log
  [& args]
  (binding [*out* *err*]
    (apply println args)))

(defn- normalize-file-path [f]
  (str/replace (.-path f) #"^\./" ""))

(defn files-same?
  "Planck's File is not very useful; this is a better equality
   function"
  [a b]
  (let [normalized-a (normalize-file-path a)
        normalized-b (normalize-file-path b)]
    (= normalized-a normalized-b)))

; ======= validation =======================================

(defn validate-directive
  "Verify that a directive is valid; throws an exception if a problem is discovered"
  [directive]
  nil)


; ======= main processing ==================================

(defn process-text
  [text]
  (loop [reader (source-logging-push-back-reader text)]
    (when-let [d (edn/read reader)]
      (validate-directive d)

      ; TODO can we strip commas in maps?
      (println (str d))

      ; keep loading directives
      (recur reader))))

(defn process-source [source-dir source-name dest-file]
  (let [files-to-process (->> source-dir
                              file-seq
                              (filter (complement directory?))
                              (remove #(str/includes? (.-path %) ".git"))
                              (filter #(str/ends-with? (.-path %) ".edn"))
                              (remove #(files-same? dest-file %))
                              (sort-by #(.-path %)))]
    (doseq [f files-to-process]
      (log "Processing " (.-path f) "...")
      (process-text (slurp f)))))

; ======= Main CLI entry point =============================

(defn- print-to-out
  [& args]
  (doseq [a args]
    (-write *out* a)))

(def cli-options
  [["-s" "--stdout" "Print to stdout instead of the file"]

   ; help
   ["-h" "--help"]])

(def usage-str "usage:
  scripts/compile-builtin-source.cljs [source-dir-name]
  scripts/compile-builtin-source.cljs [source-dir] [output-file]
")

(defn -main [& args]
  (let [{:keys [options arguments summary errors]} (parse-opts (map str args) cli-options
                                                               :in-order false)

        mode (case (count args)
               1 :builtin
               2 :custom
               3 :custom ; this is hax because right now we insert the dnd5e as the last arg
               (println "Unexpected args: " args))

        source-dir-name (first arguments)
        root (str/replace (:pwd env)
                          #"\/scripts$"
                          "")

        source-dir (case mode
                     :builtin (when source-dir-name
                                (-> root
                                    (file "resources")
                                    (file "sources")
                                    (file source-dir-name)))
                     :custom (when-let [src (first arguments)]
                               (file src)))

        dest-file (case mode
                    :builtin (when source-dir-name
                               (-> root
                                   (file "resources")
                                   (file "public")
                                   (file "sources")
                                   (file (str source-dir-name ".edn"))))
                    :custom (when-let [dst (second arguments)]
                              (file dst)))]

    (when errors
      (println errors)
      (exit 1))

    (when (:help options)
      (println usage-str)
      (println summary)
      (exit 0))

    (when-not source-dir
      (println usage-str)
      (exit 2))

    (when-not (directory? source-dir)
      (println "not a directory: " (.-path source-dir))
      (exit 3))

    ; let us redirect println
    (set! *print-fn* print-to-out)

    (log "Processing " (.-path source-dir) "...")

    (if (:stdout options)
      ; just dump everything to stdout
      (process-source source-dir source-dir-name dest-file)

      ; open the output file and bind to *out*
      (with-open [source-out (writer dest-file)]
        (binding [*out* source-out]
          (process-source source-dir source-dir-name dest-file))))))

(set! *main-cli-fn* -main)
