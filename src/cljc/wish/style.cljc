(ns ^{:author "Daniel Leong"
      :doc "Dynamic/inline style util macros"}
  wish.style
  (:require [cljs-css-modules.macro :as css]))

(defn- at-media?
  [statement]
  (and (sequential? statement)
       (= 'at-media (first statement))))

(defn insert-root
  [root-key styles]
  (let [media-styles (take-while at-media? styles)
        styles (drop-while at-media? styles)]
    (vec
      (concat
        ; at-media styles are special
        (when (seq media-styles)
          (map
            (fn [[f media & statements]]
              (list f media
                    (first
                      (insert-root root-key statements))))
            media-styles))

        ; normal styles
        (when (seq styles)
          [(into [root-key] styles)])))))

(defn- prepare-style
  [style-id [fst :as style]]
  ; based on the default css/defstyle, but without server support
  ; because we don't use it, and a bit more opinionated because it's
  ; easier—in particular: the optional first map is never a compiler
  ; options map, but instead can just be the style for "this" element)
  (let [compiler-opts {}
        style-key (keyword (str "." (name style-id)))
        css (symbol "garden.core" "css")
        inject-style-fn (symbol "cljs-css-modules.runtime" "inject-style!")
        id (gensym)
        {:keys [style map]} (reduce (partial css/create-garden-style id)
                                    {:map {}
                                     :style []}
                                    (insert-root style-key style))]
    {:class-name (get map (keyword (name style-id)))
     :inject-statement `(~inject-style-fn
                          (apply ~css ~compiler-opts ~style)
                          ~(str *ns*)
                          ~(name style-id))}))

(defmacro defclass
  "Creates a style mapping, returning the associated class name.
   The first element after the style-id is optionally a map that
   applies directly to the returned class name; the remaining
   elements should be normal garden elements, which refer to
   child elements. If you want to be more specific, you can of
   course use the normal `[:&]` syntax to refer to \"this\" element."
  [style-id & style]
  (let [{:keys [class-name inject-statement]} (prepare-style
                                                style-id
                                                style)]
    `(do
       (def ~style-id ~class-name)
       ~inject-statement)))

(defmacro defstyled
  "Convenience wrapper around (defstyle), where the returned class
   name is instead wrapped in {:class} for direct use as the
   options to an element in the common case of needing only
   a single class."
  [style-id & style]
  (let [{:keys [class-name inject-statement]} (prepare-style
                                                style-id
                                                style)]
    `(do
       (def ~style-id {:class ~class-name})
       ~inject-statement)))
