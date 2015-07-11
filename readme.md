# Meta64: A new kind of Wiki.
An Open Source "Mobile first" Wiki-type content repository built on Apache Oak JCR and MongoDb. The **meta64.com** website is currently geared towards a web developer audience, but the technology itself will eventually be a front-end for all web users.

On GitHub.com -> https://github.com/Clay-Ferguson/meta64

## Key Technologies

* Client: JavaScript, JQuery Mobile, HTML+CSS, RESTful JSON-based Ajax
* Server: Java, Spring Boot + Spring Framework, Thymeleaf, Apache Oak JCR, MongoDb and/or MySQL, Tomcat Embedded 

## Technology Demo (War and Peace)
The book "War and Peace" is stored in the repository for browsing:

http://www.meta64.com/?id=/nt:war-and-peace

Logged in users can use the Full Text Search feature, on the menu at the upper left, that appears when you log in.

## Meta64 Overview

Meta64 is at it's core a **Content Repository Browser**, or an app for interacting with hierarchical data. The website you are now reading (if you are on meta64.com) is actually running this app, and makes up everything you are seeing. The technology however is much more than a Content Browser, because it presents a GUI front end appropriate to both non-technical users, as well as the more technical users of back-end content repositories. The theory here is that "everything is content" and since both end users and technical users need to be able to interact with hierarchical data stores, it's desirable to have one system architecture that serves both roles well.

The meta64 website and content on it is now geared towards the developer and is online as a demonstration of the technology stack, but the ultimate goal is to provide capabilities very similar to Facebook, Reddit, and Wikipedia, etc. if you could imagine them all rolled into one system. With the power of Lucene and MongoDB on the backend, plus the fully standards-based open stack, this only 4-month old codebase is already able to provide incredible power only found in other systems like Wikipedia and Google, or the other large-scale content repositories that are proprietary/commercial products, and built on much older technology stacks.

For a social media user, meta64 can function as a blogging platform, file-sharing platform, social commenting platform, wiki system, personal website host, etc., while simultaneously functioning as a full-blown back-end JCR repository used by technical users like software developers, data architects, DB admins, etc. The way this is accomplished is by having a simplified set of features (i.e. rendering of the GUI) presented to non-technical users, while having the full featured JCR browser capabilities available at the flip of a switch, using a single button click. There is a 'simple' mode and 'advanced' mode, that does this.

There are just a few key concepts to know for a basic understanding of what JCR is all about, if you aren't familiar with the term:

* Everything is content, and a JCR is a Content Repository (database) standard for Java.
* Data exists as a tree structure consisting of editable nodes.
* Each user owns a part of the tree and subnodes under that make up their "account root".
* Node text and attachments can be edited (like on a Wiki)
* Markdown is used to do formatting of the nodes when displayed.
* Each node can be shared by its owner to the public or to specific users or groups.
* Nodes can be copied, cut, pasted, deleted just like in a file system.
* Any type of binary content can be uploaded onto nodes, and attached images show up as part of the page.
* Each node can be referenced by direct-linking to it on the URL, so users can publish their own pages with specific urls.
* Essentially meta64 itself is a kind of tree-structured wiki, or just a tree of editable, sharable content, just like you see on social media, but completely general purpose.

## Source Code Status
Code is "open sourced" (and on GitHub) but pre-alpha prototype currently, meaning it's not considered production ready, but does follow architectural best practices. The code is not perfectly 'clean' or perfectly organized at this time, but neither is it sloppy or low quality. It is what you would expect from a rapidly evolving prototype. Correct frameworks and architectures are in place, but some minor refactoring changes need to be done, as is true in any proof-of-concept.

## Code Ownership
Project is currently being managed and developed by Clay Ferguson (author of this document). I am very actively developing meta64 mobile, and looking for funding to continue development at some point hopefully with corporate sponsorship. I also hope to recruit other developers to join the effort and form a GitHub team who share the same goal of a state-of-the-art modern JCR Browser.

## Current Features (already working)

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

## Next Features to be Developed
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

## Technology Stack and APIs
* App is "Mobile First", meaning a primary objective is to run well on mobile
* Single Page Application (SPA)
* JavaScript+HTML+CSS Client
* Pure Java on Server
* JQuery + JQuery Mobile
* MongoDB Storage currently in use.
* MySql configuration also working
* Apache Oak JCR
* Spring MVC + Thymeleaf
* Spring Java-only configuration with Annotations, and no XML configs.
* Client-Side JavaScript Markdown (using Pagedown API) renders pages
* Entire app is Spring Boot-based. 
* Built using Maven
* Launches from a single "uber jar" containing Tomcat embedded and pre-configured

## Next Actions on the Agenda
* Will create a screencast showing a 5 minute demo, on youtube.
* Put an instance online at meta64.com, and bring down legacy site at that URL.
* Will be seeking crowdsource funding
* Hoping to get publicized at first in the JCR developer community.

## Technical Notes
* To build the app use maven.
* You need to understand 'spring boot'
* See application.properties for configurations.
* Pre-requisites: Java VM installed on machine, and MongoDB server up and running.
* Currently uses default storage location for MongoDb (Windows -> c:\data\db)
* Once app is up and running go here: http://localhost:8083 (8083 port is currently the DEV profile port)
* Remove anonUserLandingPageNode from application.properties, or set it properly. Sorry no docs exist yet on what that is, other than to say that landing page is the uuid of the page we will show all anonymous users or users before they log in.     
* Look in the 'docs' folder of the project for more documentation in addition to this readme.






