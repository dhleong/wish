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

  (create-file
    [this kind sheet-name data]
    "Create a new file with the given name, kind, and data.
     `kind` will be one of:
      - :sheet
      - :campaign")

  (disconnect!
    [this]
    "Called when the user requested to disconnect this provider")

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
    "Save string `data-str` into the the sheet with the given
     provider-specific `id`, returning the usual channel style.
     The original `data` *may* be provided, which you can use to
     update `:name`, for example, but it may also be omitted.
     `data-str` is in the same string format that should be
     returned by `load-raw`.")

  (watch-auth
    [this]
    "Generate the auth data needed to watch changes to files provided
     by this provider, or nil if that's not supported by this provider"))

(defn signed-out-err?
  "Check if the given error was caused by not being signed into the provider"
  [err]
  (when-let [info (ex-data err)]
    (= :signed-out (:state info))))
