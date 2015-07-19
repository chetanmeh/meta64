# Technical Debt

* We have numerous places in the code where we do something like delete nodes, etc, and we are going back to the server to refresh the page, when really the client-side has enough information to refresh the page without going back to server, but it takes additional work. Waiting for the feature set to become more stable before attacking these kinds of performance optimizations.

* Need to put all DOM IDs in the Const.js file, and also all other strings that are dupliate throughout the JS. Put them all in const.js file so that at least there's only one place for a typo.

# Technical Work List

* Need ability to expose a URL that can send back all the JS as one single file. Having multiple files is good for development, but for production we need to send one JS file, and also minify it eventually.

* Need to be able to block simultaneous requests from user, for example, clicking the same button twice in a row, before the first request completes. I haven't checked how JQuery handles simultaneous requests, and spring is definitely not thread-safe on the session yet, until I add that. So app is not ready for production until this work is done.

* Need some kind of progress indication of any long running processes (like an export) happening on the server.

* need menu item for 'clear selections' (remove selected nodes)

* need 'refresh' menu item that reloads current page.

* last mod times etc should show up at least in advanced editing mode

# List of Known Bugs

* nt:bin nodes (nodes that are there as a binary or image attachment) should not show up unless you are in 'advanced' mode, and they are showing up.

* password cookie key will be encrypted using AES from http://point-at-infinity.org/jsaes/, probably and probably the
https://panopticlick.eff.org/ methodology of generating a string to use for the encryption key which will be rather unique to the machine. This means if a hacker gets your cookie, it will still be difficult to decrypt, unless they can also run javascript on your machine and sent the output to their servers. Of course a hacker can lure you to their server, where they can run JS on a page and get your panopticlick info, but that is one additional challenge for them.
