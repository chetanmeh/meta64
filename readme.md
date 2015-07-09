# Meta64: A "Mobile first" JCR Browser
*(JCR=Java Content Repository)*
Document Date: 07/09/2015

## Key Technologies

* Client: JavaScript, JQuery Mobile, HTML+CSS, RESTful JSON-based Ajax
* Server: Java, Spring Boot + Spring Framework, Thymeleaf, Apache Oak JCR, MongoDb and/or MySQL, Tomcat Embedded 

## Meta64 Overview

Meta64 is a **JCR Repository Browser**, which is an app for interacting with hierarchical data. It's also much more than a JCR Browser, because it's able to present a GUI front end appropriate to non-technical users, as well as the more technical users of back-end content repositories. The theory here is that "everything is content" and since both end users and technical users need to be able to interact with hierarchical data stores, it's desirable to have one system architecture that serves both roles well, which is one of the goals of meta64.

(Point of clarification: Currently the meta64.com website is hosting the legacy version of meta64, which is an older technology stack built on GWT+JPA+MySql, and has been abandoned and replaced by the mobile version built on JCR+JQuery+MongoDb. However the news engine, email daemon, and other major non-storage components will be taken directly from the old codebase, and not rewritten.)

Now back to the new development, meta64 mobile: For a social media user, meta64 can function as a blogging platform, file-sharing platform, social commenting platform, wiki system, personal website host, etc., while simultaneously functioning as a full-blown back-end JCR repository used by technical users like software developers, data architects, DB admins, etc. The way this is accomplished is by having a simplified set of features (i.e. rendering of the GUI) presented to non-technical users, while having the full featured JCR browser capabilities available at the flip of a switch, using a single button click. There is a 'simple' mode and 'advanced' mode, that does this.

There are just a few key concepts to know for a basic understanding of what JCR is all about, if you aren't familiar with it:

* Everything is content, and a JCR is a Content Repository (database) written in Java.
* Data exists as a tree structure consisting of editable nodes.
* Each user owns a part of the tree and subnodes under that make up their "account root".
* Node text and attachments can be edited (like on a Wiki)
* Markdown is used to do formatting of the nodes when displayed.
* Each node can be shared by its owner to the public or to specific users or groups.
* Nodes can be copied, cut, pasted, deleted just like in a file system.
* Any type of binary content can be uploaded onto nodes, and attached images show up as part of the page.
* Each node can be referenced by direct-linking to it on the URL, so users can publish their own pages with specific urls.
* Essentially meta64 itself is a kind of tree-structured wiki, or just a tree of editable, sharable content, just like you see on social media, but completely general purpose.

## Source Code Status (as of date at top)
Code is "open sourced" (and on GitHub) but pre-alpha prototype currently, meaning it's not considered production ready, but does follow architectural best practices. The code is not perfectly 'clean' or perfectly organized at this time, but neither is it sloppy or low quality. It is what you would expect from a rapidly evolving prototype. Correct frameworks and architectures are in place, but some minor refactoring changes need to be done, as is true in any proof-of-concept.

## Code Ownership
Project is currently being managed and developed by Clay Ferguson (author of this document). I am very actively developing meta64 mobile, and looking for funding to continue development at some point hopefully with corporate sponsorship. I also hope to recruit other developers to join the effort and form a GitHub team who share the same goal of a state-of-the-art modern JCR Browser.

## Application Architecture

### Current Features (already working)

* Basic JCR Browser capability (Tree browsing with ability to "drill own" into the tree)
* Login/Logout
* Signup new users (including captcha, and pasword)
* Node editing (plain text/markdown)
* Orderable child nodes ("move up" and "move down" supported)
* Creating Subnodes or Inline nodes
* Full text search
* Deleting Nodes
* Moving nodes to new locations (supports multi-select)
* Sharing a node as Public, and removing shares
* Short URL GUID for any node, so it can be referenced by URL
* Uploading attachments (attached files) onto a node
* Deleting node attachments
* Renders image attachments on the page (on the node)
* Shows JCR properties for nodes.
* Allows editing of properties (single value and multivalued)
* Creating/Deleting new JCR node properties
* Can switch between simple or advanced mode editing
* Change password feature.
* Multiple nodes selectable
* Import/Export to XML
* Admin feature to insert entire book "War and Peace", for quickly
  creating test data for exploring all the features, and especially 'search'.

### Next Features to be Developed
Listed here in the order they will likely be developed...

* More JUnit unit tests.
* Ability to share node with specific user.
* Usage of email address for verification of new accounts.
* Embed legacy meta64.com news engine, as some sort of plugin, to populate news information.
* Timelining of Nodes: Defined as reverse-chronological view of all nodes recursively under a specific node.
* Display images at smaller sizes, with click to enlarge capability. Currently all images are displayed at their actual size.
* Some way to let user render text at a narrower width across the page. Lines going completely across a wide screen are hard to read.
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
* Will create a screencast showing a 5 minute demo, on youtube.
* Put an instance online at meta64.com, and bring down legacy site at that URL.
* Will be seeking crowdsource funding
* Hoping to get publicized at first in the JCR developer community.

# Technical Notes
* To build the app use maven.
* You need to understand 'spring boot'
* See application.properties for configurations.
* Pre-requisites: Java VM installed on machine, and MongoDB server up and running.
* Currently uses default storage location for MongoDb (Windows -> c:\data\db)
* Once app is up and running go here: 
     http://localhost:8083/mobile
* Remove anonUserLandingPageNode from application.properties, or set it properly. Sorry no docs exist yet on what that is, other than to say that landing page is the uuid of the page we will show all anonymous users or users before they log in.     
* Look in the 'docs' folder of the project for more documentation in addition to this readme.

# Known Bugs

 [only minor bugs known at this time]





