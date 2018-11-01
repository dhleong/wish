(ns ^{:author "Daniel Leong"
      :doc "Fn throttling based on a changing set"}
  wish.util.throttled-set
  (:require [clojure.set :refer [subset?]]))

(def ^:private initial-state {:timer nil
                              :args nil
                              :entries #{}})

(defn throttle-with-set
  "Returns a fn that accepts a set entry or collection of
   set entries and eventually calls `f` with the set of all
   entries passed to that fn. `f` is not called until the
   returned fn has not been called for `delay-ms` milliseconds.

   The returned fn also accepts N extra arguments. If any of
   these change between invocations, the previous state is
   submitted immediately. Any such extra arguments will be
   passed as with (apply) after the set of entries."
  [delay-ms f]
  (let [state (atom initial-state)
        call-f (fn f-caller []
                 (swap! state
                        (fn [{:keys [entries args]}]
                          (if args
                            (apply f entries args)
                            (f entries))

                          ; reset state:
                          initial-state)))
        defer-f #(js/setTimeout
                   call-f
                   delay-ms)]

    (fn throttled-fn [input & new-args]
      (let [input-set (if (coll? input)
                        (set input)
                        #{input})
            new-args (seq new-args)]
        (swap! state
               (fn [{:keys [timer entries args] :as old-state}]
                 (cond
                   ; new args?
                   (and timer (not= args new-args))
                   (do
                     (when timer
                       (js/clearTimeout timer))

                     ; submit any old state immediately...
                     (if args
                       (apply f entries args)
                       (f entries))

                     ; ... and defer new state
                     {:entries input-set
                      :args new-args
                      :timer (defer-f)})

                   ; nothing new? ignore
                   (subset? input-set entries)
                   old-state

                   ; new entries; cancel any old timer and start a
                   ; new one, including new entries in the pending set
                   :else
                   (do
                     (when timer
                       (js/clearTimeout timer))
                     {:entries (into entries input-set)
                      :args new-args
                      :timer (defer-f)}))))))))
