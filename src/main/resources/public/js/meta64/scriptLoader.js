console.log("running module: loader.js");

var loader = function() {

	var scriptsRemaining = 0;

	var _ = {
		loadScript : function(url, callback) {
			console.log("Requesting Script: " + url);

			return $.ajax({
				"dataType" : "script",
				"cache" : true,
				"url" : url,
				success : function(jqXHR, textStatus) {
					callback(url, textStatus);
				},
				error : function(xhr, status, error) {
					console.log("Failed loading url: " + url + " status=" + status + " error=" + error); // xhr.responseText);
					// //JSON.parse(xhr.responseText));
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
					console.log("Script Load Response for [" + url + "] (remaining=" + scriptsRemaining + " status: " + textStatus);
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
