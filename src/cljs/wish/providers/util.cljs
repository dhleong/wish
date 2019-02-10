(ns ^{:author "Daniel Leong"
      :doc "Providers util"}
  wish.providers.util)

(defn provider-id?
  "Check if the given keyword is the ID of a provider"
  [kw]
  (when kw
    (contains?
      #{:gdrive :wish}
      kw)))
