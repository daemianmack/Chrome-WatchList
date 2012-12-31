What the
-------
I've recently moved to Raleigh and discovered that living in a place people may actually mention on a webpage is a lot more fun and informative than the alternative.

Reading a ZDnet article last night about Google constraining license proliferation on Google Code (Saturday night's all right for fighting!) I happened to notice at the end that the author, Ed Burnett, wrote a book named "Hello Android". I had just seen an email to my local Android dev meetup group about that book, and, going back to read it, sure enough, same guy. He's local to me. That's kinda cool. About 30 minutes later I'm reading something on Hacker News and someone mentions Raleigh, and I'm thinking... It sure would be useful to know when a webpage contained content about my area, without me having to do a manual find on each page that looked likely. Situations of the former fuzzily associative variety I can't do much about yet, but explicit mentions of geographical regions, easy enough.

Scratching my own itch, I spent about an hour ripping off the Chrome example extension 'Sandwichbar', which notifies you if any webpage contains the text 'sandwich'. The exercise it leaves for the reader is to expand the script to notify you of *any* arbitrary regex.

That's what this is. It uses localStorage so your chosen regex is persistent.

INSTALL
-------

When this thing seems solid enough, I'll make it available in the usual manner via the Chrome webstore. Until then...

1. Wrench thingy > Tools > Extensions. 
2. Hit "Load unpacked extension..." which you'll only see if you're in developer mode. 
3. Browse to the downloaded git repo and select it. Click OK. 
4. Fiddle with the options.
5. Rejoice in a life *finally* worth living.

TODO (Known shortcomings that will be addressed upon reaching an undetermined annoyance threshold)
----

1. Domain blacklist to cut down on positives we don't care about.
2. Allow control over regex flags; I want global and case-insensitive, so that's what you get for now.
3. Allow multiple regexes, instead of dealing with a single unwieldy regex.
