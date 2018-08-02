Scripting Gotchas
=================

To keep scripting working without having to add several *megabytes*
of javascript, we need to use some special tricks. Unfortunately,
this means that some things you might expect to work from Clojure
or Clojurescript do not work exactly the same:

- The available functions are severely limited. I'm not maintaining
    a separate list just yet, but most of the basics and things
    you *probably* need are included. If there's a function that
    you absolutely need and isn't available, open an Issue to request
    it—or better yet, submit a PR!
- You cannot always use data structures as functions. Special handling
    exists for the common `(:key map)` and `(some #{:set} coll)` cases,
    but in general you will have to avoid these patterns in favor
    of explicit function calls (eg: `(get)` or `(contains?)`)
- `(and)` will only ever return `true` or `false`, which is quite
    different from normal. This probably 
