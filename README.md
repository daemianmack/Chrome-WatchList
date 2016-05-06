# What the

This Chrome extension lets you specify arbitrary terms (regexes too).

When your terms appear on a webpage, they will be highlighted. 

A summary of them will appear in a statusbar, and clicking the terms in the statusbar will scroll the page to the place that term appears.

It uses Chrome's sync API for Storage so your options are persistent across all your devices.



# Pictures > Words

![demo.gif](https://raw.githubusercontent.com/daemianmack/Chrome-WatchList/master/resources/doc/demo.gif)



# Installation

Watchlist is available in the usual manner via the Chrome webstore. 



# Digging in

This is a ClojureScript jam. You'll want [lein](http://leiningen.org/).

##### Installing from source

First, make sure you've run `lein dev` or `lein prod`, and then in Chrome...

1. Wrench thingy > Tools > Extensions. 
2. Hit "Load unpacked extension..." which you'll only see if you're in
   developer mode.
3. Locate the downloaded git repo and select `builds/dev`. Click OK. 
4. Fiddle with the options.
5. Rejoice in a life *finally* worth living.

#### Dev build

`lein dev` will start watching the source directories for changes, and will automatically place fresh builds into `builds/dev`. 

The dev build uses `:whitespace` optimization.

#### Prod build

Similarly to the dev build, `lein prod` will start watching the source directories for changes, and will automatically place fresh builds into `builds/prod`. 

The prod build uses `:advanced` optimization.

#### Testing

`lein autotest` will start watching the source and test directories for changes, automatically re-running the tests with each change.



# TODO 

Allow control over regex flags; I want case-insensitive, so
that's what you get for now.

Allow grouping of regexes with distinct CSS rules for each group.

Allow control over the CSS that informs the statusbar.



[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/daemianmack/chrome-watchlist/trend.png)](https://bitdeli.com/free
"Bitdeli Badge")
