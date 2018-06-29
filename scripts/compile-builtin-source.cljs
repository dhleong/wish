#!/usr/bin/env bash
"exec" "plk" "-Sdeps" "{:deps {org.clojure/tools.cli {:mvn/version \"0.3.5\"}}}" "-Ksf" "$0" "$@" "dnd5e"
(ns wish.builtin-source-compiler
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.reader.reader-types :refer [source-logging-push-back-reader]]
            [clojure.string :as str]
            [cljs.reader :as edn]
            [planck.core :refer [exit file-seq *err* slurp with-open]]
            [planck.environ :refer [env]]
            [planck.io :refer [directory? file writer]]))

(defn log
  [& args]
  (binding [*out* *err*]
    (apply println args)))


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

(defn process-source [source-dir source-name]
  (let [files-to-process (->> source-dir
                              file-seq
                              (filter (complement directory?))
                              (remove #(let [p (.-path %)]
                                         (re-matches #".*\.sw[nop]$" p)))
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

(def usage-str "usage: scripts/compile-builtin-source.cljs [source-dir-name]")

(defn -main [& args]
  (println args)
  (let [{:keys [options arguments summary errors]} (parse-opts (map str args) cli-options
                                                               :in-order false)
        source-dir-name (first arguments)
        root (str/replace (:pwd env)
                          #"\/scripts$"
                          "")
        source-dir (when source-dir-name
                     (-> root
                         (file "resources")
                         (file "sources")
                         (file source-dir-name)))

        dest-file (when source-dir-name
                    (-> root
                        (file "resources")
                        (file "public")
                        (file "sources")
                        (file (str source-dir-name ".edn"))))]

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
      (process-source source-dir source-dir-name)

      ; open the output file and bind to *out*
      (with-open [source-out (writer dest-file)]
        (binding [*out* source-out]
          (process-source source-dir source-dir-name))))))

(set! *main-cli-fn* -main)
