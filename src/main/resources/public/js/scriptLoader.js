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
					 * If there is a syntax error in the script this will not be
					 * able to show the line number, however we are using Google
					 * Closure Compiler to detect script problems, so if this
					 * happens during development, simply running the builder
					 * will list any JS errors on the console.
					 */
					console.log("ERROR: url=" + url + " status[" + status + "] error: " + error);
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
