console.log("running module: pageMgr.js");

var pageMgr = function() {

	/*
	 * Contains a mapping of all page names and the functions to call to
	 * generate that page. Each page initializer will usually only be run one
	 * time to generate the HTML.
	 */
	var pageBuilders = null;

	var _ = {

		buildPage : function(pageName) {
			for (var i = 0; i < pageBuilders.length; i++) {
				var builder = pageBuilders[i];
				// console.log("Checking page builder: "+builderObj.name);
				if (pageName.contains(builder.name)) {
					// console.log("found page builder.");
					
					if (!builder.built) {
						// console.log("building page.");
						builder.build();
						builder.built = true;
					}

					if (builder.init) {
						builder.init();
					}
					break;
				}
			}
		},

		initializePageBuilders : function() {
			if (pageBuilders) {
				console.log("initializePageBuilders called twice ?");
				return;
			}

			pageBuilders = [ {
				name : "#signupPg",
				build : signupPg.build
			}, {
				name : "#loginPg",
				build : loginPg.build,
				init : loginPg.init
			}, {
				name : "#prefsPg",
				build : prefsPg.build
			}, {
				name : "#changePasswordPg",
				build : changePasswordPg.build
			}, {
				name : "#exportPg",
				build : exportPg.build
			}, {
				name : "#importPg",
				build : importPg.build
			}, {
				name : "#searchPg",
				build : searchPg.build
			}, {
				name : "#uploadPg",
				build : uploadPg.build,
				init : uploadPg.init
			}, {
				name : "#sharingPg",
				build : sharingPg.build,
				init : sharingPg.init
			}, {
				name : "#shareToPersonPg",
				build : shareToPersonPg.build,
				init : shareToPersonPg.init
			}, {
				name : "#searchResultsPg",
				build : searchResultsPg.build,
				init : searchResultsPg.init
			} ];
		}
	};

	console.log("Module ready: pageMgr.js");
	return _;
}();

// # sourceURL=pageMgr.js
