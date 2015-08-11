console.log("running module: prefs.js");

var prefs = function() {

	var _ = {
		savePreferencesResponse : function(res) {
			if (util.checkSuccess("Saving Preferences", res)) {
				meta64.jqueryChangePage("#mainPage");
				view.scrollToSelectedNode();
			}
		},

		closeAccountResponse : function() {
			/* Remove warning dialog to ask user about leaving the page */
			$(window).off("beforeunload");

			/* reloads browser with the query parameters stripped off the path */
			window.location.href = window.location.origin;
		},

		closeAccount : function() {

			confirmPg.areYouSure("Oh No!", "Close your Account? Are you sure? This was so unexpected!", "Yes, Close Account.", function() {
				util.json("closeAccount", {}, _.closeAccountResponse);
			});
		},

		savePreferences : function() {
			meta64.editModeOption = $("#editModeSimple").is(":checked") ? meta64.MODE_SIMPLE : meta64.MODE_ADVANCED;

			util.json("saveUserPreferences", {
				"userPreferences" : {
					"advancedMode" : meta64.editModeOption === meta64.MODE_ADVANCED
				}
			}, _.savePreferencesResponse);
		},

		populatePreferencesPg : function() {
			$('#editModeSimple').prop('checked', meta64.editModeOption === meta64.MODE_SIMPLE).checkboxradio('refresh');
			$('#editModeAdvanced').prop('checked', meta64.editModeOption === meta64.MODE_ADVANCED).checkboxradio('refresh');
		},

		accountPreferencesPg : function() {

			meta64.changePage(prefsPg);
			_.populatePreferencesPg();
		}
	};

	console.log("Module ready: prefs.js");
	return _;
}();

// # sourceURL=prefs.js
