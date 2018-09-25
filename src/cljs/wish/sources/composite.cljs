(ns ^{:author "Daniel Leong"
      :doc "composite"}
  wish.sources.composite
  (:require-macros [wish.util.log :as log])
  (:require [wish.sources.compiler :refer [install-features]]
            [wish.sources.compiler.race :refer [install-deferred-subraces]]
            [wish.sources.core :refer [->CompositeDataSource ->DataSource raw]]
            [wish.util :refer [->set process-map]]))

; ======= last-step deferred combinations =================

(defn- resolve-subraces [sources]
  (let [s (->> sources
               (map raw)
               (map #(select-keys % [:races :deferred-subraces]))
               (apply merge-with merge))
        subrace-ids (->> s :deferred-subraces keys)]
    (-> s
        install-deferred-subraces
        (update :races select-keys subrace-ids)
        (dissoc :deferred-subraces))))

(defn- combine-sources [sources]
  ; create a temporary source combining the provided sources
  ; that we can use to find features for the resolved subraces,
  ; if any
  (let [tmp-source (->CompositeDataSource :tmp sources)]
    (->> sources
         resolve-subraces

         (process-map :races #(install-features %1 %2 tmp-source)))))


; ======= public interface ================================

(defn composite-source
  [id sources]
  (if (not= (count sources) 1)
    (->CompositeDataSource
      id
      (conj sources

            ; build an extra data source with parts
            ; combined from provided sources
            (->DataSource
              (keyword "composited-" (name id))
              (combine-sources sources))))

    ; if there's only one source, don't bother wrapping it
    (first sources)))
