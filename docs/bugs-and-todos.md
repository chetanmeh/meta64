# Technical Debt
* We have serval places in the code where we do something like delete nodes, etc, and we are going back to the server to refresh the page, when really the client-side has enough information to refresh the page without going back to server, but it takes additional work. Waiting for the feature set to become more stable before attacking these kinds of performance optimizations.


# TODO
* need 'delete account' capability where user can leave meta64, and have all their data deleted from the server.
* currently searching only searches the 'jcr:content' property, for faster performance than searching all properties, but more advanced and configurable search options can be added in the future.
* search results that have no results should show a message instead of just blank page!
* search results header bar can scroll off screen. make it fixed at top just like main page header.
  (attempting this caused problems, so the code is backed out, for now, by being commented out)
* after changing edit mode (simple/advanced) need to refresh entire page from server
* on search results screen, need a button to directly edit a node from there.
* Search and Timeline features are so important they deserve buttons at top left just beside the menu button.
* Edit button is not showing on page parent node (by design) but I need to show it I think.
* Need row headers with metadata like (lastUpdateTime, owner, etc)
* For nodes that are shared, they should be indicated in some obvious way to the user, without having to go to sharing page to check.
* Need edit email address feature.
* Need to implement "forgot my password" feature to send user password.
* If user is logged in we should hide the "signup" button.
* Need menu item that will display anonymous home page to a user who is logged in.
* Need to be able to block simultaneous requests from user, for example, clicking the same button twice in a row, before the first request completes. I haven't checked how JQuery handles simultaneous requests, and spring is definitely not thread-safe on the session yet, until I add that. So app is not ready for production until this work is done.
* Need some kind of progress indication of any long running processes (like an export) happening on the server.
* need menu item for 'clear selections' (remove selected nodes)
* need 'refresh' menu item that reloads current page.
* last mod times etc should show up at least in advanced editing mode
* Add user documentation as actual content, and create a button that links to that in a separate browser window/tab.
* SEO (Search Engine Optimization): Need to make home page detect Google web crawler, and expose urls that include mainly the home page of meta64 content but also allows WebCrawling of **one** page per user, and have a standard of like **/user/home** being the folder that users can create that will be 'searchable' on google.
* Admin console that shows free memory statistics, connection info, number of logged in users, disk space consumption specifically in the DB storage folder, etc.
* Embed legacy meta64.com news engine, as some sort of plugin, to populate news information.
* Timelining of Nodes: Defined as reverse-chronological view of all nodes recursively under a specific node.
* Some way to let user render text at a narrower width across the page. Lines going completely across a wide screen are hard to read - at least on a larger screen device
* Need "Move to Top" and "Move to Bottom" in addition to "up"/"down"
* More JUnit unit tests.

# List of Known Bugs
* sometimes when I click on the top node (page parent node) it is unable to select the node (no red indicator shows on left). Saw this happen when I accessed a node via url that was shared from another user. Also happens when you click Home button.
* password cookie key will be encrypted using AES from http://point-at-infinity.org/jsaes/, probably and probably the
https://panopticlick.eff.org/ methodology of generating a string to use for the encryption key which will be rather unique to the machine. This means if a hacker gets your cookie, it will still be difficult to decrypt, unless they can also run javascript on your machine and sent the output to their servers. Of course a hacker can lure you to their server, where they can run JS on a page and get your panopticlick info, but that is one additional challenge for them.
