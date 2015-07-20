console.log("running module: prefs.js");

var prefs = function() {

	var _ = {
		savePreferencesResponse : function(res) {
			if (util.checkSuccess("Saving Preferences", res)) {
				$.mobile.changePage("#mainPage");
				view.scrollToSelectedNode();
			}
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
			
			$.mobile.changePage("#accountPreferencesPg");
			_.populatePreferencesPg();
		}
	};

	console.log("Module ready: prefs.js");
	return _;
}();

//# sourceURL=prefs.js
