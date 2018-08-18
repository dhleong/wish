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
   features to be limited. The spec can take a few forms:
   - number: If a number is applied, the compiled function
             accepts the input iff (<= (count features) number)
   - (fn [features]): If one of the arguments is named `features`,
                      the function is expected to return a truthy
                      value when the provided features collection
                      is valid, and falsey otherwise
   - (fn []): If `features` is not one of the arguments, the function
              is expected to return a number based on the provided
              state, which is then treated the same as the `number`
              case above."
  [o]
  (when o
    (cond
      ; already compiled
      (fn? o) o

      (number? o) (with-meta
                    (fn [{:keys [features]}]
                      (<= (count features) o))
                    {:const o})

      (and (list? o)
           (= 'fn (first o))
           (-> o
               second
               set
               (contains? 'features))) (->callable o)

      (and (list? o)
           (= 'fn (first o))) (let [f (->callable o)]
                                (fn [{:keys [features] :as state}]
                                  (let [max-options (f state)]
                                    (<= (count features)
                                        max-options))))

      :else #(log/warn "Invalid :max-options " o))))

(defn compile-feature
  "Compile a feature map"
  [fm]
  ; TODO how can sheets declare keys that should be callable?
  (-> fm
      (update :max-options compile-max-options)
      (update :values-filter ->callable)
      (update :available? ->callable)
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
