(ns ^{:author "Daniel Leong"
      :doc "Providers util"}
  wish.providers.util)

; lazy resolve to avoid circular dependencies;
; we need to know if a keyword is a provider-id for, eg checking if a
; keyword value is a sheet ID in subs-util, but all sorts of things depend
; on subscriptions—including wish.providers (indirectly) so to prevent
; circular dependencies, we resolve it (and memoize the result) at runtime.
; We *could* just hard-code the provider IDs here, but that duplication
; seems unnecessarily dumb
(def ^:private get-info-ref (delay (resolve 'wish.providers/get-info)))

(defn provider-id?
  "Check if the given keyword is the ID of a provider"
  [kw]
  (when kw
    (not (nil?
           (@get-info-ref
             kw)))))
