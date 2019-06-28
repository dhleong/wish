(ns ^{:author "Daniel Leong"
      :doc "Gdrive config styles"}
  wish.providers.gdrive.styles
  (:require [wish.style :refer-macros [defclass defstyled]]))

(defstyled signin-button
  {:cursor 'pointer
   :display 'inline-block}

  [".focus,.pressed" {:display 'none}]
  [:&:hover
   [:.focus {:display 'block}]
   [:.normal {:display 'none}]]
  [:&:active
   [:.focus {:display "none !important"}]
   [:.normal {:display 'none}]
   [:.pressed {:display 'block}]])
