#List of Known Bugs

* password cookie key will be encrypted using AES from http://point-at-infinity.org/jsaes/, probably and probably the
https://panopticlick.eff.org/ methodology of generating a string to use for the encryption key which will be rather unique to the machine. This means if a hacker gets your cookie, it will still be difficult to decrypt, unless they can also run javascript on your machine and sent the output to their servers. Of course a hacker can lure you to their server, where they can run JS on a page and get your panopticlick info, but that is one additional challenge for them.