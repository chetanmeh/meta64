# Technical Debt
* We have serval places in the code where we do something like delete nodes, etc, and we are going back to the server to refresh the page, when really the client-side has enough information to refresh the page without going back to server, but it takes additional work. Waiting for the feature set to become more stable before attacking these kinds of performance optimizations.
* Need to put all DOM IDs in the Const.js file, and also all other strings that are dupliate throughout the JS. Put them all in const.js file so that at least there's only one place for a typo.

# TODO
* Need menu item that will display anonymous home page to a user who is logged in.
* Need to be able to block simultaneous requests from user, for example, clicking the same button twice in a row, before the first request completes. I haven't checked how JQuery handles simultaneous requests, and spring is definitely not thread-safe on the session yet, until I add that. So app is not ready for production until this work is done.
* Need some kind of progress indication of any long running processes (like an export) happening on the server.
* need menu item for 'clear selections' (remove selected nodes)
* need 'refresh' menu item that reloads current page.
* last mod times etc should show up at least in advanced editing mode
* Add user documentation as actual content, and create a button that links to that in a separate browser window/tab.
* SEO (Search Engine Optimization): Need to make home page detect Google web crawler, and expose urls that include mainly the home page of meta64 content but also allows WebCrawling of **one** page per user, and have a standard of like **/user/home** being the folder that users can create that will be 'searchable' on google.
* Admin console that shows free memory statistics, connection info, number of logged in users, disk space consumption specifically in the DB storage folder, etc.
* Usage of email address for verification of new accounts and notifications of sharing, etc..
* Embed legacy meta64.com news engine, as some sort of plugin, to populate news information.
* Timelining of Nodes: Defined as reverse-chronological view of all nodes recursively under a specific node.
* Some way to let user render text at a narrower width across the page. Lines going completely across a wide screen are hard to read - at least on a larger screen device
* Email notification engine to support collaboration
* Need "Move to Top" and "Move to Bottom" in addition to "up"/"down"
* More JUnit unit tests.

# List of Known Bugs
* after uploading attachment image, viewing it, then deleting it, and uploading another, browser caching us using the
same version number as the original. Solution: need to use random generated number for binary version rather than, a
sequential one. Will still work fine, but no risk of reusing the same URL again.
* **bin** nodes (nodes that are there as a binary or image attachment) should not show up unless you are in 'advanced' mode, and they are showing up.
* password cookie key will be encrypted using AES from http://point-at-infinity.org/jsaes/, probably and probably the
https://panopticlick.eff.org/ methodology of generating a string to use for the encryption key which will be rather unique to the machine. This means if a hacker gets your cookie, it will still be difficult to decrypt, unless they can also run javascript on your machine and sent the output to their servers. Of course a hacker can lure you to their server, where they can run JS on a page and get your panopticlick info, but that is one additional challenge for them.
