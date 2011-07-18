I've recently moved to Raleigh and discovered that living in a place people may actually mention on a webpage is a lot more fun and informative than the alternative.

Reading a ZDnet article last night about Google constraining license proliferation on Google Code (Saturday night's all right for fighting!) I happened to notice at the end that the author, Ed Burnett, wrote a book named "Hello Android". I had just seen an email to my local Android dev meetup group about that book, and, going back to read it, sure enough, same guy. He's local to me. That's kinda cool. About 30 minutes later I'm reading something on Hacker News and someone mentions Raleigh, and I'm thinking... It sure would be useful to know when a webpage contained content about my area, without me having to do a manual find on each page that looked likely. Situations of the former fuzzily associative variety I can't do much about yet, but explicit mentions of geographical regions, easy enough.

Scratching my own itch, I spent about an hour ripping off the Chrome example extension 'Sandwichbar', which notifies you if any webpage contains the text 'sandwich'. The exercise left for the reader is to expand the script to notify you of *any* arbitrary regex.

That's what this is. It uses localStorage so your chosen regex is persistent.

I've left the original Sandwichbar code in as initial commit so you can see how trivial Chrome extensions are.


TODO
----
1. Domain blacklist to cut down on positives we don't care about.
2. Allow control over regex flags; I want global and case-insensitive, so that's what you get for now.
3. Find someone who cares about 1 and 2. Also number 3.