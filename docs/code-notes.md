#JavaScript + Client-side Conventions

## Single Page Application (SPA)

The app is a SPA and the main loading page is 'index.html', which is technically rendered by Spring+Thymeleaf, but it contains JQuery Mobile code in it, which is what does most of the magic. Once the initial page is loaded, REST/AJAX calls do the rest of the work of updating the page content HTML as the user navigates around the app. The same DIVs are simply reloaded with new content as new information is queried from the server and gotten back as JSON.

## Underscore (_)

A underscore variable in the JS should generally be interpreted at "this". In other words an underscore (_) at the front of a varible or as a scoping variable, means that the variable is referencing the current JS file that it's in. The convention would mean that for example, in a class named 'util.js' if you see something like "_.myVar or _.myFunction" those will be the same as "util.myVar or util.myFunction". So the underscore itself is the scoping variable that is the near equivalent of 'this' in Java or C++. Since JS is a functional language more than an OOP one, we use this name convention. I think this is a common convention used by JS developers also, to have underscore mean something similar to 'this'.

## RESTfulness

The AJAX calls to the server are all done using JSON POSTs that get JSON back in the response also. This means that when we are retrieving data to display on the page, we are also getting back the minimal raw data objects from the server (like POJOS), and then rendering them into HTML on the browser side. This is not 100% true because we do have certain scaffolding that is in pure HTML (with some Thymeleaf assisting its creation) and is sent back from the server as HTML, but this is mainly (or only) ever done during the initial page load when JQuery Mobile pages and Dialogs are first constructed and wired up. 

## Modularity Pattern and Variable Scope

Each JS file has a variable that is creates and returns as a function. This is done for scoping primarily. Since JS has no concept of classes and scopes (outside functions) we use this pattern which is also a widely used pattern for JS code. So when you see something like 'render.doSomething()' you know right away this is a method in the 'render.js' file, without having to do any searching. Also the general need for hiding variables into the 'most localized' scope you can is accomplished this way, and is the best practice.

