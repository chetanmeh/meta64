console.log("running module: pageMgr.js");

var pageMgr = function() {

	var _ = {
	/*
		 * Contains a mapping of all page names and the functions to call to
		 * generate that page. Each page initializer will usually only be run
		 * one time to generate the HTML.
		 */
		pageBuilders : [],
		
		buildPage : function(pageName) {
			for (var i = 0; i < _.pageBuilders.length; i++) {
				var builder = _.pageBuilders[i];
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
			if (_.pageBuilders.length > 0) {
				console.log("initializePageBuilders called twice ?");
				return;
			}

			_.pageBuilders.push({
				name : "#signupPg",
				build : signupPg.build,
				built : false,
				init : null
			});
			
			_.pageBuilders.push({
				name : "#loginPg",
				build : loginPg.build,
				built : false,
				init : loginPg.init
			});
			
			_.pageBuilders.push({
				name : "#prefsPg",
				build : prefsPg.build,
				built : false,
				init : null
			});
			
			_.pageBuilders.push({
				name : "#changePasswordPg",
				build : changePasswordPg.build,
				built : false,
				init : null
			});
			
			_.pageBuilders.push({
				name : "#exportPg",
				build : exportPg.build,
				built : false,
				init : null
			});
			
			_.pageBuilders.push({
				name : "#importPg",
				build : importPg.build,
				built : false,
				init : null
			});
		}
	};

	console.log("Module ready: pageMgr.js");
	return _;
}();

// # sourceURL=pageMgr.js
