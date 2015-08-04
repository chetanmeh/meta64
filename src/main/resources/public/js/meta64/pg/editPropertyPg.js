console.log("running module: editPropertyPg.js");

var editPropertyPg = function() {

	var _ = {
		domId : "editPropertyPg",

		build : function() {

			var header = render.makeTag("div", //
			{
				"data-role" : "header"// ,
			// "data-position" : "fixed",
			// "data-tap-toggle" : "false"
			}, //
			"<h2>" + BRANDING_TITLE + " - Edit Node Property</h2>");

			var savePropertyButton = render.makeButton("Save", "savePropertyButton", "b", "ui-btn-icon-left ui-icon-check");
			var cancelEditButton = render.makeBackButton("Cancel", "editPropertyPgCloseButton", "a");
			var buttonBar = render.makeHorzControlGroup(savePropertyButton + cancelEditButton);

			var internalMainContent = "<div id='editPropertyPathDisplay' class='path-display-in-editor'></div>" + //
			"<div id='addPropertyFieldContainer' class='ui-field-contain'></div>";

			var mainContent = render.makeTag("div", //
			{
				"role" : "main", //
				"class" : "ui-content"
			}, //
			internalMainContent + buttonBar);

			var content = header + mainContent;

			util.setHtmlEnhanced($("#editPropertyPg"), content);

			$("#savePropertyButton").on("click", props.saveProperty);
		},

		init : function() {
			props.populatePropertyEdit();
		}
	};

	console.log("Module ready: editPropertyPg.js");
	return _;
}();

// # sourceURL=editPropertyPg.js
