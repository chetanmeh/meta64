# Meta64 Mobile
Document Date: 6/28/2015

## Overview Meta64

Meta64 is a **JCR repository browser**, and an app for interacting with hierarchical data. However it's much more than a JCR Browser, because it's able to present a GUI front end appropriate to non-technical users, as well as the usual technical users of back-end content repositories. The theory here is that "everything is content" and since both end users and technical users need to be able to interact with hierarchical data stores, it should be possible to have one system architecture that serves both roles well. 

For a social media user, meta64 can function as a blogging platform, file-sharing platform, social commenting platform, wiki system, personal website host, etc., while simultaneously functioning as the full-blown back-end JCR repository used by more technical users (software developers, data architects, DB admins, etc.) The way this is accomplished is by having a simplified set of features (i.e. rendering of the GUI) presented to non-technical users, while having the full featured JCR browser capabilities available to everyone also.

There are only a few key concepts which must be understood to know what a JCR is all about, if you aren't familiar with the term:

* Data exists as a tree structure, of editable nodes.
* Each user owns a part of the tree and subnodes under that make up their "account" or "data".
* Nodes can be edited (like on a Wiki)
* Markdown used to do formatting of the nodes when displayed.
* Each node can be shared by its owner to the public or to specific users or groups.
* Nodes can be copied, cut, pasted, deleted just like in a file system.
* Any type of binary content can be uploaded onto nodes. (Images render right inline, and any other file type displays as a download link)
* Each node can be referenced by direct-linking to it on the URL

## Code Status (as of date at top)
Code is "open sourced" but pre-alpha prototype currently, meaning it's not considered production ready, but does follow architectural best practices. The code is not perfectly 'clean' or perfectly organized at this time, but neither is it sloppy or low quality. It is what you would expect from a rapidly evolving prototype. Correct frameworks and architectures are in place, but some minor refactoring changes need to be done, as is true in any proof-of-concept.

## Code Ownership
Project is currently being managed and developed by Clay Ferguson, also the author of this document. (I am very actively developing meta64 mobile, and looking for funding to continue development at some point hopefully with full corporate sponsorship. I also hope to recruit other developers to join the effort and form a team who share the same goal for a good state-of-the-art modern JCR Browser, built on Java/Spring middleware and JQuery Mobile client.

----

## Application Architecture

### Current Features (already existing)

* Basic JCR Browser capability (Tree browsing with ability to "drill own" into the tree)
* Login/Logout
* Signup new users (including captcha, and pasword)
* Node editing (plain text/markdown)
* Orderable child nodes ("move up" and "move down" supported)
* Creating Subnodes or Inline nodes
* Deleting Nodes
* Sharing a node as Public, and removing shares
* Short URL GUID for any node, so it can be referenced by URL
* Uploading attachments (attached files) onto a node
* Deleting node attachments
* Renders image attachments on the page (on the node)
* Shows JCR properties for nodes.
* Allows editing of properties (single value and multivalued)
* Creating new JCR node properties
* Can switch between simple or advanced mode editing
* Change password feature.
* Multiple nodes selectable
* Mobile ready (this is a "mobile first" app)

### Next Features to be Developed
Listed here in the order they will likely be developed...

* Cut, Copy, Paste (move nodes to new location on tree)
* Ability to share node with specific user.
* Ability to Export to XML, and Import from XML.
* Usage of email address for verification of new accounts.
* Full text search
* Embed legacy meta64.com news engine, as some sort of plugin, to populate news information.
* Create importer for book War and Peace, to demo full-text searching capability.
* Timelining of Nodes: Defined as reverse-chronological view of all nodes recursively under a specific node.
* Display images at smaller sizes, with click to enlarge capability. Currently all images are displayed at their actual size.
* Email notification engine to support collaboration
* RSS Feeds for node changes

### Technology Stack and APIs
* App is "Mobile First", meaning a primary objective is to run well on mobile
* Single Page Application (SPA)
* Pure JavacScript+HTML+CSS (client)
* Pure Java on Server
* JQuery + JQuery Mobile
* MongoDB Storage currently in use.
* MySql configuration also working
* Apache Oak JCR
* Spring MVC + Thymeleaf
* Spring Java-only configuration with Annotations, and no XML configs.
* Client-Side Javascript Markdown (using Pagedown API) renders pages
* Entire app is Spring Boot-based. 
* Built using Maven
* Launches from a single "uber jar" containing Tomcat embedded and pre-configured

### Next Actions on the Agenda
* Put an instance online at meta64.com, and bring down legacy site at that URL.
* Will be creating a youtube page with screencasts showing use of the app
* Will be seeking crowdsource funding
* Hoping to get publicized at first in the JCR developer community.

# Technical Notes
* To build the app use maven.
* You need to understand 'spring boot' to know the basic architecture, and why no pre-existing Tomcat is needed.
* See application.properties for configurations.
* Pre-requisites: Java VM installed on machine, and MongoDB server up and running.
* Currently uses default storage location for MongoDb (Windows -> c:\data\db)
* Once app is up and running go here: 
     http://localhost:8083/mobile
* Remove anonUserLandingPageNode from application.properties, or set it property. Sorry no docs exist yet on what that is, other than to say that landing page is the uuid of the page we will show all anonymous users or users before they log in.     






