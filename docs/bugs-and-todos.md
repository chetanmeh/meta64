# Technical Debt
* We have serval places in the code where we do something like delete nodes, etc, and we are going back to the server to refresh the page, when really the client-side has enough information to refresh the page without going back to server, but it takes additional work. Waiting for the feature set to become more stable before attacking these kinds of performance optimizations.

# TODO
* Adding a new record is taking way over one second. Nearly two. Which is noticeable. This started happening when lucene indexes were added and warnings are getting logged during this time, so there's something we aren't quite doing right in here I think.
* Need to implement "forgot my password", and reset password without sending actual password to user, but instead a limited duration (like 5minutes) random code that can be used to login and change password.
* need 'delete account' capability where user can leave meta64, and have all their data deleted from the server.
* search results that have no results should show a message instead of just blank page!
* search results header bar can scroll off screen. make it fixed at top just like main page header.
  (attempting this caused problems, so the code is backed out, for now, by being commented out)
* after changing edit mode (simple/advanced) need to refresh entire page from server
* on search results screen, need a button to directly edit a node from there.
* Edit button is not showing on page parent node (by design) but I need to show it I think.
* Need row headers with metadata like (lastUpdateTime, owner, etc)
* For nodes that are shared, they should be indicated in some obvious way to the user, without having to go to sharing page to check.
* Need edit email address feature.
* Need menu item that will display anonymous home page to a user who is logged in.
* Need some kind of progress indication of any long running processes (like an export) happening on the server.
* last mod times etc should show up at least in advanced editing mode
* Add user documentation as actual content, and create a button that links to that in a separate browser window/tab.
* SEO (Search Engine Optimization): Need to make home page detect Google web crawler, and expose urls that include mainly the home page of meta64 content but also allows WebCrawling of **one** page per user, and have a standard of like **/user/home** being the folder that users can create that will be 'searchable' on google.
* Admin console that shows free memory statistics, connection info, number of logged in users, disk space consumption specifically in the DB storage folder, etc.
* Embed legacy meta64.com news engine, as some sort of plugin, to populate news information.
* Some way to let user render text at a narrower width across the page. Lines going completely across a wide screen are hard to read - at least on a wide screen device
* Need "Move to Top" and "Move to Bottom" in addition to "up"/"down"
* More JUnit unit tests.
* If user spends a very long time editing a node without saving, and the session times out they can lose ability to save. Need intermittent save to a property like "content-unsaved" so that when they open the editor, the next time we can detect this, and ask them if they would like to continue editing their unsaved work.

# List of Known Bugs
* sometimes when I click on the top node (page parent node) it is unable to select the node (no red indicator shows on left). Saw this happen when I accessed a node via url that was shared from another user. Also happens when you click Home button.
* password cookie key will be encrypted using AES from http://point-at-infinity.org/jsaes/, probably and probably the https://panopticlick.eff.org/ methodology of generating a string to use for the encryption key which will be rather unique to the machine. This means if a hacker gets your cookie, it will still be difficult to decrypt, unless they can also run javascript on your machine and sent the output to their servers. Of course a hacker can lure you to their server, where they can run JS on a page and get your panopticlick info, but that is one additional challenge for them.
