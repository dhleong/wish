(ns ^{:author "Daniel Leong"
      :doc "Feature compiler"}
  wish.sources.compiler.feature
  (:require-macros [wish.util.log :as log])
  (:require [wish.sources.core :as src]
            [wish.sources.compiler.entity :refer [compile-entity]]
            [wish.sources.compiler.fun :refer [->callable]]))

(defn- attrs->availability
  [attrs availability-attr]
  (not
    (if (keyword? availability-attr)
      (get attrs availability-attr)
      (get-in attrs availability-attr))))

(defn- uses-available??
  [raw-fn]
  (when (seq? raw-fn)
    (let [[_fn args & _] raw-fn]
      (when (vector? args)
        (some #{'available?} args)))))

(defn compile-available
  [base-raw {:keys [availability-attr]}]
  (cond
    ; easy case: neither fn nor attr
    (not (or base-raw availability-attr))
    nil

    ; normal case: fn with no attr
    (and base-raw (not availability-attr))
    (->callable base-raw)

    ; simple case: attr but no fn
    (and availability-attr (not base-raw))
    (fn [{:keys [attrs]}]
      (attrs->availability attrs availability-attr))

    ; tricky case: both!
    :else
    (let [base-fn (->callable base-raw)
          explicit-combine? (uses-available?? base-raw)]
      (if explicit-combine?
        (fn [{:keys [attrs] :as args}]
          (base-fn (assoc args
                          :available? (attrs->availability
                                        attrs availability-attr))))

        ; it doesn't explicitly use the `available?` key,
        ; so combine it implicitly
        (fn [{:keys [attrs] :as args}]
          (and (attrs->availability
                 attrs availability-attr)
               (base-fn args)))))))

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

(defn add-availability
  [directives attr-id-or-path]
  (let [provide-directive [:!provide-attr attr-id-or-path true]]
    (if directives
      (conj directives provide-directive)
      [provide-directive])))

(def compile-desc-memoized (memoize ->callable))
(def compile-desc (fn [d]
                    (if (string? d)
                      d
                      (compile-desc-memoized d))))

(def compile-values-filter (memoize ->callable))

(defn compile-feature
  "Compile a feature map"
  [fm]
  ; TODO how can sheets declare keys that should be callable?
  (-> fm
      (update :desc compile-desc)
      (update :max-options compile-max-options)
      (update :values-filter compile-values-filter)
      (update :available? compile-available fm)

      (cond->
        (:availability-attr fm) (update :!
                                        add-availability
                                        (:availability-attr fm)))

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
