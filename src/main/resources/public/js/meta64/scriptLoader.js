console.log("running module: loader.js");

/*
 * Performs dynamic/programmatic loading of multiple JavaScript files
 */
var loader = function() {

	var scriptsRemaining = 0;

	var _ = {
		loadScript : function(url, callback) {
			console.log("Requesting Script: " + url);

			return $.ajax({
				"dataType" : "script",
				"cache" : true,
				"url" : url,
				success : function(xhr, textStatus) {
					callback(url, textStatus);
				},
				error : function(xhr, status, error) {
					/*
					 * If script fails because of a syntax error you will get an
					 * error message below, but no line number. I spent several
					 * hours trying to google and find out how to get line
					 * number for a syntax error and it appears to be absolutely
					 * impossible, even though it seems easy. If you think i'm
					 * wrong just go ahead and try it! :) I'd love to be proven
					 * wrong.
					 * 
					 * Will have to find some fancy LINTER to help out with line
					 * numbers.
					 */
					console.log("ERROR: url=" + url + " status[" + status + "] error[" + error);
				}
			});
		},

		/*
		 * scripts: array of script names (urls)
		 */
		loadScripts : function(scripts, allScriptsLoaded) {
			var len = scripts.length;
			console.log("Loading " + len + " scripts.");
			scriptsRemaining += len;

			for (var i = 0; i < len; i++) {
				var script = scripts[i];
				_.loadScript(script + "?ver=" + cacheVersion, //
				function(url, textStatus) {
					scriptsRemaining--;
					console.log("Script Load Response for [" + url + "] remaining=" + scriptsRemaining + " status: " + textStatus);
					if (scriptsRemaining == 0) {
						console.log("All scripts loaded!");
						allScriptsLoaded();
					}
				});
			}
		}
	};

	console.log("Module ready: loader.js");
	return _;
}();

// # sourceURL=scriptLoader.js
