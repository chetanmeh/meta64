## Single Page Application (SPA)

The app is a SPA and the main loading page is 'index.html', which is technically rendered by Spring+Thymeleaf, but it contains JQuery Mobile code in it, which is what does most of the magic. Once the initial page is loaded, REST/AJAX calls do the rest of the work of updating the page content HTML as the user navigates around the app. The same DIVs are simply reloaded with new content as new information is queried from the server and gotten back as JSON.

## JSON/Ajax Server Calls

The AJAX calls to the server are all done using JSON POSTs that get JSON back in the response also. This means that when we retrieve data to display on the page, we are getting back the minimal raw data objects from the server (like POJOS), and then rendering them into HTML on the browser side mostly. The only time the server returns HTML is during the initial page load of the index.html file. From then on it's pure dynamic javascript on complete control of the page. The application is not using a REST interface, because the URL itself does not always refer to a resource on the server, but instead is a 'command name'. So the JSON/Ajax is a "Command Pattern" rather than a "Resource Naming" convention as dictated by REST.

## Modularity Pattern and Variable Scope

Each JS file has a variable that is creates and returns as a function. This is done for scoping primarily. Since JS has no concept of classes and scopes (outside functions) we use this pattern which is also a widely used pattern for JS code. So when you see something like 'render.doSomething()' you know right away this is a method in the 'render.js' file, without having to do any searching. Also the general need for hiding variables into the 'most localized' scope you can is accomplished this way, and is the best practice.

## Underscore (_)

A underscore variable in the JS should generally be interpreted at "this". In other words an underscore (_) at the front of a varible or as a scoping variable, means that the variable is referencing the current JS file that it's in. The convention would mean that for example, in a class named 'util.js' if you see something like "_.myVar or _.myFunction" those will be the same as "util.myVar or util.myFunction". So the underscore itself is the scoping variable that is the near equivalent of 'this' in Java or C++. Since JS is a functional language more than an OOP one, we use this name convention. I think this is a common convention used by JS developers also, to have underscore mean something similar to 'this'.

## Markdown 

Meta64 allows markdown text to be entered into content fields and renders them using standard markdown syntax. The markdown formatter api we use is PageDown.

https://code.google.com/p/pagedown/

This is a javascript port of Markdown, as used on Stack Overflow and the rest of Stack Exchange network. Largely based on showdown.js by John Fraser (Attacklab), and original Markdown Copyright (c) 2004-2005 by John Gruber
http://daringfireball.net/projects/markdown/

## IMPORTANT!!! - JavaScript Caching Mechanism

To understand how cache lifetime is managed for JS files see the following: *SpringMvcUtil.addJsFileNameProp*. In general the approach is to leave the JS files always the same, but to use a different URL parameter to be able to control which files are read from the browser cach v.s. downloaded new. Each JS url looks something like this:

    /js/meta64/util.js?ver=1

## JavaScript Loading

Most all javascript is loaded using an 'ajax' query. The main file 'index.html' is responsible for loading the files. It also detects if the system is running as 'production' Spring Profile, and if so then it only loads one single minified script file containing all the meta64 javascript, minified by the **Google Closure JS Compiler**, here's the command that minifies the script. Currently I am running this in a batch file that runs before the maven builder:

    java -jar google-compiler.jar --js_output_file="[root]\src\main\resources\public\js\meta64.min.js" "[root]\src\main\resources\public\js\meta64\**.js"

Note that in the command above you of course need your own path insead of **[root]**.
See also: https://developers.google.com/closure/compiler/

Also: If you want to ignore the minification stuff to get started using meta64, just change this string:

    (profileName === 'prod')
To this... (in index.html)

    (profileName === '')

## JQuery + JQueryMobile Javascript CDN

The default configurtion in the spring config files is to specify the CDN (Content Delivery Network) location of the minified versions of the jars if you are running in PRODUCTION profile, but for TEST and DEV profiles, it loads the normal (non minified) jars, and loads them from local folder.

## Starting MongoDB

Commands to run MongoDb (on Windows)

	 Windows: 
	 mongod --dbpath c:\mongodb-data\db --port 27017
	 
Use CTRL-C to terminate the app gracefully.

## Running the Meta64 Server (on Windows)

* Note that the spring.config.location argument (as in the command below) you are required to include the main application.properties file as well as one of the profile ones (ending in -test, -dev, or -production)

    Example Windows BAT file to run Server:
    set JAVA_HOME=C:\Program Files\Java\jdk1.7.0_55
    set PATH=%JAVA_HOME%\bin;%PATH%
    java -jar com.meta64.mobile-0.0.1-SNAPSHOT.jar 
    	 --jcrAdminPassword=yourPasswordHere
       --spring.config.location=classpath:/application.properties, 
    	   classpath:/application-test.properties
       --mail.user=name@someserver.com 
       --mail.password=youremailserverpassword 
       --mail.host=somehost.someserver.net
       --aeskey=9999999999999999
    
## Encryption Key Config

For temporarily storing passwords before the user is created in the repository the following key needs to be supplied:

    --aeskey=9999999999999999

This key must be exactly 16 characters long. It's used in an AES algorithm. (See Encryptor.java)

## Warning about Eclipse Code Formatter

The Eclipse code formatter will break the 'sourceURL' file name desgination in the javascript files by always putting a space between '//' and '#' as shown here:

This works:

    //# sourceURL=renameNodePg.js

This is broken (by eclipse)

    // # sourceURL=renameNodePg.js

I have no yet found a way to configure the eclipse to stop doing this, and it is a very serious problem. Only thing I know to do is eventually use ant text file replace feature to be able to fix it as part of the build process. Without having the sourceURL the stack traces for javascript errors are even more unhelpful than normal!




