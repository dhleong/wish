(ns wish.style.media)

(def tiny {:screen :only
           :max-width "375px"})
(def smartphones {:screen :only
                  :max-width "479px"})
(def not-smartphones {:min-width "480px"})
(def tablets {:max-width "1024px"})
(def laptops {:min-width "1100px"})

