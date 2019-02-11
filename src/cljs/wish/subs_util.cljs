(ns ^{:author "Daniel Leong"
      :doc "subs-util"}
  wish.subs-util
  (:require-macros [wish.util.log :as log :refer [log]])
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [wish.providers.util :refer [provider-id?]]))

(defn active-sheet-id
  [db & [page-vec]]
  (let [page-vec (or page-vec
                     (:page db))]
    (let [[page args] page-vec]
      (case page
        :campaign (first args)
        :join-campaign (second args)
        :sheet args
        :sheet-builder (first args)

        ; else, no sheet
        nil))))

(defonce ^:private id-subs (atom #{}))
(defn- id-sub? [query-vec]
  (let [query-id (first query-vec)]
    (or (= :active-sheet-id query-id)
        (contains? @id-subs query-id))))

(defn query-vec->preferred-id [query-vec]
  ; NOTE: We can't just use (last), since if no args are passed
  ; that will just return the query-id
  (let [candidate (second query-vec)]
    ; make sure it's actually a sheet id
    (when (and (keyword? candidate)
               (provider-id? (keyword (namespace candidate)))
               (= "w" (first (name candidate))))
      candidate)))

(defn inject-preferred-id [vec preferred-sheet-id]
  (if preferred-sheet-id
    (conj vec preferred-sheet-id)
    vec))

(defn reg-id-sub
  "This is a drop-in replacement for a subscription which
   ultimately depends on [:active-sheet-id], which optionally
   takes a single parameter in the query vector indicating
   the sheet-id that should be used instead of the result of
   [:active-sheet-id]"
  [query-id & args]
  (let [computation-fn (last args)
        input-args (butlast args)
        err-header (str "(reg-id-sub " query-id ")")
        inputs-fn (case (count input-args)
                    ; error case
                    0 nil

                    ; single function; just pass through
                    1 (let [base-fn (first input-args)]
                        (fn inp-fn [query-vec]
                          (let [base-subs (base-fn query-vec)
                                actual-sheet-id (query-vec->preferred-id query-vec)]
                            ; verify sanity
                            (if-not (every? vector? base-subs)
                              (do (log/err err-header "should return query vectors, not subscriptions")
                                  ; use them, I guess
                                  base-subs)

                              (->> base-subs
                                   (map #(inject-preferred-id % actual-sheet-id))
                                   (map subscribe))))))

                    ; single sugar pair
                    2 (let [[marker vec] input-args]
                             (when-not (= :<- marker)
                               (log/err err-header "expected :<-, got:" marker))
                             (fn inp-fn [query-vec]
                               (let [actual-sheet-id (query-vec->preferred-id query-vec)]
                                 (subscribe (inject-preferred-id vec actual-sheet-id)))))

                    ; multiple sugar pairs
                    (let [pairs (partition 2 input-args)
                          markers (map first pairs)
                          vecs (map last pairs)
                          any-id-subs? (some id-sub? vecs)]
                      (when-not (and (every? #{:<-} markers) (every? vector? vecs))
                        (log/err err-header "expected pairs of :<- and vectors, got:" pairs))

                      (fn inp-fn [query-vec]
                        (let [actual-sheet-id (query-vec->preferred-id query-vec)]
                          (->> vecs
                               (map #(inject-preferred-id % actual-sheet-id))
                               (map subscribe)))))
                    )]
    (if-not inputs-fn
      (log/warn err-header "must have input args")

      (do
        (swap! id-subs conj query-id)
        (reg-sub
          query-id
          inputs-fn
          computation-fn)))))
