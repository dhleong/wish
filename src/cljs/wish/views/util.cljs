(ns ^{:author "Daniel Leong"
      :doc "View/widget utils"}
  wish.views.util)

(defn dispatch-change-from-keyup
  "Event handler that be used for :on-key-up
   that dispatches on-change immediately."
  [e]
  (let [el (.-target e)]
    (js/console.log e)
    (js/console.log el)
    (js/console.log  (.-onchange el))
    (js/console.log  (.-oninput el))
    (js/console.log  (.-onblur el))
    (.dispatchEvent
      el
      (doto (js/document.createEvent "HTMLEvents")
        (.initEvent "input" true false)))))
