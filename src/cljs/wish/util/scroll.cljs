(ns ^{:author "Daniel Leong"
      :doc "Scroll utils"}
  wish.util.scroll)

(defn find-scroll-parent [el]
  (when el
    (let [client-height (.-clientHeight el)]
      (if (and (not= 0 client-height)
               (> (.-scrollHeight el) client-height))
        el

        (recur (.-parentElement el))))))

(defn scrolled-amount [el]
  (let [scrolling (when-let [scrolling-el js/document.scrollingElement]
                    (.-scrollTop scrolling-el))]
    (if (> scrolling 0)
      scrolling

      (when-let [parent (find-scroll-parent el)]
        (.-scrollTop parent)))))
