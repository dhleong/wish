(ns ^{:author "Daniel Leong"
      :doc "Core types, etc for Provider"}
  wish.providers.core)

(defprotocol IProvider
  "Anything that can load data"
  (id [this])
  (init!
    [this]
    "Perform any necessary init, possibly eager-fetching sheet ids
     from cache or something")

  (create-sheet
    [this sheet-name data]
    "Create a new sheet with the given name and data")

  (load-raw
    [this id]
    "Load raw data with the given provider-specific `id`. This method
     can be used to load the raw text from directive files when loading
     a data-source.

     This returns a channel that emits `[err data]`, where `err`
     will be nil on success.")

  (load-sheet
    [this id]
    "Load the sheet with the given provider-specific `id`.

     This returns a channel that emits `[err data]`, where `err`
     will be nil on success.")

  (save-sheet
    [this id data]
    "Save `data` into the the sheet with the given provider-specific
     `id`, returning a channel that emits nil on success or an
     error value on failure"))

