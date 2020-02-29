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

Wish is built and developed using [shadow-cljs][7]. There is a separate
build target for the `:app` and the service `:worker`; you will probably
want to attach to the `:app` target.

I develop in Vim, and recommend using [vim-fireplace][2]. You should be
able to connect to the repl within [shadow-cljs][7] using its normal
methods.  [My dotfiles][3] have fairly extensive customization if you want
a place to start.

### Getting started

First, you'll need to get a [node.js][8] environment setup with `npm`.
Then, simply run `npm install` to install [shadow-cljs][7] and all the
Javascript dependencies.

### Compile css

Compile css file once:

```
npm run build:css
```

CSS also gets automatically built when running `npm run build`. Note that
we are migrating away from using Less for CSS, and instead will be using
[spade][9] for all styling going forward.

### Compile builtin Data Sources:

You'll need to do this on first checkout and any time you update part of a data source:

```
scripts/compile-builtin-sources
```

This script uses [wish-compiler][4] under the hood, which can be used to compile custom Data Sources for homebrew, etc. [Included in this repo][6] is a wrapper script which automatically downloads the latest version of [wish-compiler][4]. If you use this script and need to update your local copy of the binary, just delete `.bin/wish-compiler` and the script will re-fetch it for you.

### Run application:

```
npm run dev
```

shadow will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3450](http://localhost:3450).

This will keep the shadow server running in the background, for quick
restarts. To stop the shadow server, use `npm run stop`. If you prefer not
to keep the shadow server running, you can instead use `npm run watch`.

### Run tests:

```
npm run test
```

The above assumes that you have Chrome installed for running the tests.


## Production Build

To compile clojurescript to javascript:

```
npm run build:simple
```

For deploying to github pages, we use:

```
npm run build
```

[1]: https://github.com/Day8/re-frame
[2]: https://github.com/tpope/vim-fireplace
[3]: https://github.com/dhleong/dots/blob/master/.vim/ftplugin/clojure.vim
[4]: https://github.com/dhleong/wish-compiler
[5]: http://dnd.wizards.com/articles/features/systems-reference-document-srd
[6]: https://github.com/dhleong/wish/blob/master/scripts/wish-compiler
[7]: https://shadow-cljs.github.io/docs/UsersGuide.html
[8]: https://nodejs.org
[9]: https://github.com/dhleong/spade
