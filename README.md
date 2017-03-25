# What the

This Chrome extension lets you specify arbitrary terms, including regexes.

When your terms match text on a webpage, that text will be highlighted.

A summary of matches will appear in a statusbar at the bottom of the window, and clicking the terms in the statusbar will scroll the page to the place that match appears.

It uses Chrome's sync API for Storage so your options are persistent across all your devices.



# Pictures > Words

![demo.gif](https://raw.githubusercontent.com/daemianmack/Chrome-WatchList/master/resources/doc/demo.gif)



# Installation

Watchlist is available in the usual manner [via the Chrome webstore](https://chrome.google.com/webstore/detail/watchlist/menehddibpdfhbfgfmhkddgbajijoole?hl=en-US&gl=US).



# Digging in

This is a ClojureScript jam. You'll want [lein](http://leiningen.org/).

##### Installing from source

First, make sure you've run `lein dev` or `lein prod`, and then in Chrome...

1. Wrench thingy > Tools > Extensions.
2. Hit "Load unpacked extension..." which you'll only see if you're in developer mode.
3. Locate the downloaded git repo and select the new build under `target`. Click OK.
4. Fiddle with the options.
5. Rejoice in a life *finally* worth living.

##### Testing

`lein cljsbuild once test`

#### Dev build

`lein dev` will start watching the source directories for changes, and will automatically place fresh builds into `target/unpacked`. Assets in `resources/assets` will be copied in once at the beginning but will not be monitored for changes; this mode has the shortest cycle time and is sort of the guerilla-mode option.

#### Prod build

`lein prod` will place a fresh production build in `target/prod`.

#### Test build

`lein autotest` will start watching the source and test directories
for changes, automatically re-running the tests with each change.
`phantomjs` must be on the `PATH`.

There is a set of thumb-in-the-wind performance benchmark scenarios
that can be chosen at test runtime via an env var...

`PERF=true lein autotest`

#### All builds
`lein chromebuild auto` will conveniently auto-execute all builds, including `test`, and will monitor `resource/assets` for changes; it is not fast, is included mainly for novelty and should be replaced with `lein auto` or similar if fast-feedback asset development becomes helpful again.



# TODO

Allow control over regex flags; I want case-insensitive, so
that's what you get for now.

Allow grouping of regexes with distinct CSS rules for each group.
