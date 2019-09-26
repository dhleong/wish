(ns ^{:author "Daniel Leong"
      :doc "Gdrive config styles"}
  wish.providers.gdrive.styles
  (:require [spade.core :refer [defattrs]]))

(defattrs signin-button []
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
