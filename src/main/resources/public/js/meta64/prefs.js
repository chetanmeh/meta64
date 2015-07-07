console.log("running module: prefs.js");

var prefs = function() {
	
	var _ = {
		savePreferences : function() {
			meta64.editModeOption = $("#editModeSimple").is(":checked") ? meta64.MODE_SIMPLE : meta64.MODE_ADVANCED;

			view.refreshTree();
			$.mobile.changePage("#mainPage");
			view.scrollToSelectedNode();
		},

		accountPreferencesDialog : function() {
			console.log("changing page to accountPreferencesDialog");
			$.mobile.changePage("#accountPreferencesDialog");
		}
	};

	console.log("Module ready: prefs.js");
	return _;
}();
