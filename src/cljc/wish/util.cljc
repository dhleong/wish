(ns ^{:author "Daniel Leong"
      :doc "Util macros"}
  wish.util)

(defmacro fn-click
  "Declares a click handler. This is a drop-in replacement
   for the special `fn` when declaring :on-click function that
   should (.preventDefault) on the provided event.

   When used like:

     (fn-click (println \"hi\"))

   this is equivalent to:

     #(do (.preventDefault %)
          (println \"hi\"))

   If you need access to the event, you can optionally supply
   a bindings vector, eg:

     (fn-click [e]
       (.stopPropagation e))"
  [& [bindings :as body]]
  (if (and (vector? bindings)
           (> (count body) 1)
           (= (count bindings) 1))
    ; including bindings!
    `(fn ~bindings
       (.preventDefault ~(first bindings))
       ~@(rest body))

    ; drop argument
    `(fn [e#]
       (.preventDefault e#)
       ~@body)))
