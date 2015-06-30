console.log("running module: prefs.js");

var prefs = function() {
	/*
	 * ================= PUBLIC =================
	 */
	var _ = {
		savePreferences : function() {
			meta64.js.editModeOption = $("#editModeSimple").is(":checked") ? meta64.js.MODE_SIMPLE : meta64.js.MODE_ADVANCED;

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
