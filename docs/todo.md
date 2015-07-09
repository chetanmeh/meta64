## TODO 

# Technical Debt

* review TODOs in code.

# Technical Work List

* Need to make create and update times and user properties not appear as editable properties, and just display as some small read-only
text on the node. Currently these times/users completely clutter up the edit page.

* Need ability to expose a URL that can send back all the JS as one single file. Having multiple files is good for development, but for production we need to send one JS file, and also minify it eventually.

* Add Spring profiles for DEV and PROD at a minimum.

* Need to be able to block simultaneous requests from user, for example, clicking the same button twice in a row, before the first
request completes. I haven't checked how JQuery handles simultaneous requests, and spring is definitely not thread-save on the session yet, 
until I add that. So app is not ready for production until this work is done.

* Need some kind of progress indication of any long running processes (like an export) happening on the server.
