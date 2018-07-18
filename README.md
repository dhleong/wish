# wish [![Build Status](http://img.shields.io/travis/dhleong/wish.svg?style=flat)](https://travis-ci.org/dhleong/wish)

*The character sheets we've always wished for*

## What?

The WISH project aims to create the kind of dynamic, living character
sheet we’ve always wished for. To achieve this goal, WISH divides
character sheets into several parts:

1. **Sheet Data** is just the data specific to a character.
2. **The Sheet renderer** is the actual set of [re-frame][1] views,
subscriptions, etc. that render the character sheet. This part is
responsible for implementing the system-specific logic, such as
health management, etc. Renderers don't know any specific information
(such as that the Wizard class can cast spells) but they do know
how to handle things generally (such as managing a Wizard's spells
and magic use).
3. **Data Sources** provide all the specific information that a Sheet
Renderer needs to function. Any given sheet may have one or more
Data Sources, and they all combine together so that the sheet has
all the information it needs. They can be updated without having to
change the Sheet Data to get the latest changes.
4. **Providers** handle saving and loading Sheet Data and Data Sources,
both of which are basically plain text files. WISH does not store your
data itself, but rather uses Providers like Google Drive (currently the
only provider) so you are in complete control of your data.

WISH sheets will generally come with a builtin data source that provides
the core, publically available bits to get you started. Right now we
support D&D 5th Edition, and our (incomplete) data source is based on
the [System Reference Document][5].

## Development Mode

### Using Vim:

I recommend using [vim-fireplace][2]. You should be able to connect
to the repl within Figwheel using the normal methods. [My dotfiles][3]
have fairly extensive customization if you want a place to start.

### Start Cider from Emacs:

Put this in your Emacs config file:

```
(setq cider-cljs-lein-repl
	"(do (require 'figwheel-sidecar.repl-api)
         (figwheel-sidecar.repl-api/start-figwheel!)
         (figwheel-sidecar.repl-api/cljs-repl))")
```

Navigate to a clojurescript file and start a figwheel REPL with `cider-jack-in-clojurescript` or (`C-c M-J`)

### Compile css:

Compile css file once.

```
lein less once
```

Automatically recompile css file on change.

```
lein less auto
```

CSS also gets automatically built when running `lein build`.

### Compile builtin Data Sources:

You'll need to do this on first checkout and any time you update part of a data source:

```
scripts/compile-builtin-sources
```

This script uses [planck][4] to execute clojurescript without the extra
compile steps, and can be used to compile custom Data Sources for homebrew,
etc. At some point we may just pull this tool out into a separate project
since we have to build planck from source as part of the CI deploy process
anyway, so it's not exactly saving time....

### Run application:

```
lein figwheel
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

### Run tests:

```
lein clean
lein npm install
lein test
```

The above assumes that you have Chrome installed for running the tests. `lein-npm` is used to install the Karma test runners for executing the tests.

## Production Build


To compile clojurescript to javascript:

```
lein build
```

[1]: https://github.com/Day8/re-frame
[2]: https://github.com/tpope/vim-fireplace
[3]: https://github.com/dhleong/dots/blob/master/.vim/ftplugin/clojure.vim
[4]: https://github.com/planck-repl/planck
[5]: http://dnd.wizards.com/articles/features/systems-reference-document-srd
