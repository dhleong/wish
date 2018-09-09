(ns ^{:author "Daniel Leong"
      :doc "Core types, etc for Provider"}
  wish.providers.core)

(defprotocol IProvider
  "Anything that can load data"
  (id [this])
  (init!
    [this]
    "Perform any necessary init, returning a channel that emits the state of
     the Provider, such as `:ready`, `:signed-out`, etc. init! may be called
     multiple times throughout the lifetime of the app, but usually only if
     it previously returned `:unavailable`. In such cases, it is acceptable
     for the Provider to just return a channel that immediately emits its
     current state, if there's no need or ability to retry init. If the
     Provider did previously fail to init (and emitted :unavailable), such
     a case should be considered a good opportunity to try again.
     See the comments on `:provider-states` in `db.cljs` for all acceptable
     states.")

  (create-sheet
    [this sheet-name data]
    "Create a new sheet with the given name and data")

  (load-raw
    [this id]
    "Load raw data with the given provider-specific `id`.

     This returns a channel that emits `[err data]`, where `err`
     will be nil on success.")

  (register-data-source
    [this]
    "Launch a UX flow to register a new data source from this provider")

  (query-data-sources
    [this]
    "Query for known data sources, storing into the DB directly")

  (query-sheets
    [this]
    "Query for known character sheets, returning a channel that emits
     `[err data]`, where `data` is a sequence of {:id,:name,:mine?}.
     :id should already be formatted as per `wish.sheets.util/make-id`")

  (save-sheet
    [this id data data-str]
    "Save `data` into the the sheet with the given provider-specific
     `id`, returning a channel that emits nil on success or an
     error value on failure. `data` is pre-formatted as `data-str`,
     which is what you should write for symmmetry with `load-raw` and
     caching. However, the original `data` is provided in case you
     want to pull out `:name`, for example."))

