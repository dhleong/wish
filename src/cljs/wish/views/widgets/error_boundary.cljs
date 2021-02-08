(ns wish.views.widgets.error-boundary
  (:require [archetype.views.error-boundary :as archetype]
            [clojure.string :as str]))

(defn- clean-error [e]
  (let [top-message (ex-message e)
        e (or (:error (ex-data e))
              e)

        message (ex-message e)
        stack (if message
                (.-stack e)
                (str e))]
    (->> [(when-not (= top-message message)
            top-message)

          (when-not (str/starts-with?
                      stack
                      message)
            message)

          (str/replace stack #"\n(\n)+" "\n..\n")]

         ; format:
         (keep identity)
         (str/join "\n"))))

(defn- clean-component-stack [stack]
  ; stacks of nameless div and Components are not terribly helpful...
  ; might be a quirk of safari because chrome doesn't do this...
  (-> stack
      (str/replace #"\nComponent(\nComponent)+" "\nComponent ..")
      (str/replace #"\ndiv(\ndiv)+" "\ndiv ..")))

(def ^:private props {:clean-error clean-error
                      :clean-component-stack clean-component-stack})

(defn error-boundary [& children]
  (into [archetype/error-boundary props] children))
