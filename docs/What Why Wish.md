What? Why? Wish.
================

This document will provide a deep dive into the important questions surrounding WISH, its origins, and its design. If you hope to contribute to WISH or its community, either through code or by creating Data Sources, you're in the right place. If you're a player just trying to figure out why everything looks so ugly, I can also help you out: it was designed by an engineer, not an artist.

## What?

WISH is a system-agnostic framework for presenting dynamic, *data-centric* character sheets. Well gee, what the fork does that mean? Let's break it down:

### System agnostic

WISH in general doesn't care whether you're playing Fantasy AGE or Dungeons and Dragons. At a high level, WISH provides some basic, primitive structures for implementing a game system, like storage for character sheet data, level-based features, etc., but leaves the system-specific stuff like how much health a character should have to the Sheet Renderer. So, provided someone builds a Sheet Renderer for your system, it can be represented on WISH.

### Framework

While the UI and logic behind a Sheet Renderer is up to whoever's building it, WISH provides everything else, like saving and loading character sheets, providing access to sheets offline, etc.

### Dynamic

No erasers needed! WISH lets you build character sheets that can change and evolve with your character, with powerful support for "leveling up" and gaining new skills.

### Data-centric

This part is key: while you'll need a Sheet Renderer to see your character sheet, you also need a Data Source or two in order to have anything to put *on* that sheet. WISH provides a rich domain language for creating the Data Sources that provide the content for your system.

Nothing exists in a character sheet that cannot be represented in a Data Source—or, to put it differently, everything that exists in the game system, exists as a Data Source. Classes, subclasses, skills, spells, items—everything. This means that homebrewers can create new classes that behave just like built-in classes, because nothing is hard-coded in a way that cannot be extended.

An interesting benefit of this approach is that re-skinned systems—like building a Star Wars RPG on top of 5th Edition D&D—can make use of the Sheet Renderer's logic by just swapping out the core Data Source—neat, right?

## Why?

My original attempt at this concept was with [sheater][1], which took the "dynamic" part to an extreme. Sheater was built when I convinced my friends to play a [Fantasy AGE][2] game set in the [Titansgrave][3] universe. It was the first tabletop game of this sort for most of us, and while Excel spreadsheets are nice, I wanted something easier to use, something more powerful.

With Sheater, the structure and logic of a character sheet was combined with the data. While this made it very flexible, it also meant that if the maintainer of a system added a new item, or fixed a typo in a spell, you wouldn't see it. It also meant that if someone came up with a clever new layout, you wouldn't easily be able to use it.

WISH builds on what I learned from Sheater, taking the dynamic, programmable sheets concept and separating the character data from the system data, and the system data from the system.

### Why Clojure(script)?

Because I love it. That's pretty much it. Once you get into it, [re-frame][4] is an amazingly simple, powerful, and elegant way to build an application.

For a more concrete example, if you insist: Clojure is purpose-built to manipulate data. It's a data-centric language! So, it's a natural choice for a data-centric sheet.

### Why EDN?

Data Sources (and character sheet data) are stored as [EDN][5]. The easy explanation here is that WISH is built in Clojurescript, and EDN is a subset of the Clojure language, so it's the obvious choice.

But we can go a bit futher: as a subset of Clojure, EDN can naturally embed structures that look like Clojure functions, so it's straightforward to make Sheet data programmatic, which was a key goal.

### Why Google Drive?

I wanted users to be fully in control of their data. I also stored data in Google Drive with Sheater, but I took it a step further with WISH and stored it as normal files instead of hiding it in the Application-specific data folder. This means you can see exactly what we're storing in the files, edit them by hand if something goes wrong, and delete them when you no longer want them.

If you don't like Google Drive, that's fine—while it's currently the only Provider, WISH was designed so that Providers—the things that *provide* storage for your data—can be easily swapped out. Perhaps you'll be the one to add a Provider for whatever service you prefer!

This is also related to the next question:

### Why Free and Open Source?

Through playing Titansgrave and, later, D&D, I've discovered that these types of games are truly amazing things, allowing you to experience stories that even modern video games can't quite convey, and to connect with people around the world in ways that few other media allow.

Watching the community grow around [Critical Role][6] and seeing how positive and open the larger role-playing community is, I wanted to give back: with sheets that can not only evolve with your character, but also with direct, transparent community input. I wanted to provide a way to get into this hobby that didn't hit your wallet, or your privacy (which is why there are no ads, either!).

So, WISH is hosted as a Github Page, and you host your own data.

Now, this does not mean that you shouldn't support the creators of the systems whose sheets can be used here. [Wizards of the Coast][7] did an amazing job with 5th Edition, and it's incredible that they released enough of the system for us to freely create what we've created here. If you enjoy using their system, you should absolutely buy their books and support them! That's why WISH generally has somewhat less of the rules hand-holding than Wizards' official [D&D Beyond][8].

In fact, go ahead and check out [D&D Beyond][8]—it's great! When I started WISH it did not work nearly as well as it does not, or I might not have kept going with WISH. If you find yourself coming back to WISH, awesome! If you like D&D Beyond better, that's awesome too! As long as you've found a way to get involved in this awesome hobby, WISH has done its job.

[1]: https://github.com/dhleong/sheater
[2]: https://greenroninstore.com/products/fantasy-age-basic-rulebook
[3]: https://geekandsundry.com/shows/titansgrave/
[4]: https://github.com/Day8/re-frame
[5]: https://github.com/edn-format/edn
[6]: https://critrole.com/
[7]: https://dnd.wizards.com/
[8]: https://dndbeyond.com
