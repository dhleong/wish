(ns ^{:author "Daniel Leong"
      :doc "Feature compiler"}
  wish.sources.compiler.feature
  (:require-macros [wish.util.log :as log])
  (:require [wish.sources.core :as src]
            [wish.sources.compiler.entity :refer [compile-entity]]
            [wish.sources.compiler.fun :refer [->callable]]
            [wish.util :refer [->map]]))

(defn compile-max-options
  ":max-options compiles to an acceptor function that
   expects `{:features []}`, where :features is the list of
   features to be limited."
  [o]
  (when o
    (cond
      ; already compiled
      (fn? o) o

      (number? o) (fn [{:keys [features]}]
                    (<= (count features) o))

      ;; (vector? o) ; TODO support filters list whenever we have it

      (and (list? o)
           (= 'fn (first o))) (->callable o)

      :else #(log/warn "Invalid :max-options " o))))

(defn compile-feature
  "Compile a feature map"
  [fm]
  ; TODO how can sheets declare keys that should be callable?
  (-> fm
      (update :max-options compile-max-options)
      compile-entity))

(defn- ->feature [state f]
  (cond
    (map? f) (compile-feature f)
    (keyword? f) (let [data-source (:wish/data-source state)]
                   (or (get-in state [:features f])
                       (when data-source
                         (src/find-feature data-source f))
                       (log/warn "Could not find feature " f)))
    :else (log/warn "Unexpected feature def " (type f) f)))

(defn inflate-features
  "Given a state and a collection of either feature ids or
   feature-maps, inflate them as appropriate and return the
   resulting collection"
  [state items]
  (->> items
       (map (partial ->feature state))))
