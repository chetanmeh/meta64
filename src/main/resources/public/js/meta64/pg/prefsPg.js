console.log("running module: prefsPg.js");

var prefsPg = function() {

	var _ = {
		build : function() {

			var header = render.makeTag("div", //
			{
				"data-role" : "header"//,
				//"data-position" : "fixed",
				//"data-tap-toggle" : "false"
			}, //
			"<h2>" + BRANDING_TITLE + " - Account Peferences</h2>");

			var formControls = render.makeRadioButton("Simple", "editModeRadioGroup", "editModeSimple", true) + //
			render.makeRadioButton("Advanced", "editModeRadioGroup", "editModeAdvanced", false);
			var legend = "<legend>Edit Mode:</legend>";
			var radioBar = render.makeHorzControlGroup(legend+formControls);
			
			var saveButton = render.makeButton("Save", "savePreferencesButton", "b", "ui-btn-icon-left ui-icon-check");
			var backButton = render.makeBackButton("Cancel", "cancelPreferencesPgButton", "a");
			var buttonBar = render.makeHorzControlGroup(saveButton + backButton);

			var form = render.makeTag("div", //
			{
				"class" : "ui-field-contain" //
			}, //
			radioBar + buttonBar);

			var internalMainContent = "";
			var mainContent = render.makeTag("div", //
			{
				"role" : "main", //
				"class" : "ui-content"
			}, //
			internalMainContent + form);

			var content = header + mainContent;

			util.setHtmlEnhanced($("#prefsPg"), content);
			$("#savePreferencesButton").on("click", prefs.savePreferences);
		}
	};

	console.log("Module ready: prefsPg.js");
	return _;
}();

// # sourceURL=prefsPg.js
