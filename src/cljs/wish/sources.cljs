(ns ^{:author "Daniel Leong"
      :doc "Data Sources"}
  wish.sources)

;; TODO this is probably different in prod
(def data-root "/sources")

(def builtin-sources
  {:wish/dnd5e-srd "/dnd5e.edn"})
