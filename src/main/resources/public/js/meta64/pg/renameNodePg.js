console.log("running module: renameNodePg.js");

var renameNodePg = function() {

	var _ = {
		domId : "renameNodePg",

		build : function() {

			var header = render.makeTag("div", //
			{
				"data-role" : "header"// ,
			// "data-position" : "fixed",
			// "data-tap-toggle" : "false"
			}, //
			"<h2>" + BRANDING_TITLE + " - Rename Node</h2>");

			var formControls = render.makeEditField("Enter new name for the node", "newNodeNameEditField");

			var renameNodeButton = render.makeButton("Rename", "renameNodeButton", "b");
			var backButton = render.makeBackButton("Close", "cancelRenameNodeButton", "a");
			var buttonBar = render.makeHorzControlGroup(renameNodeButton + backButton);

			var form = render.makeTag("div", //
			{
				"class" : "ui-field-contain" //
			}, //
			formControls + buttonBar);

			var internalMainContent = "";
			var mainContent = render.makeTag("div", //
			{
				"role" : "main", //
				"class" : "ui-content"
			}, //
			internalMainContent + form);

			var content = header + mainContent;
			util.setHtmlEnhanced($("#renameNodePg"), content);

			$("#renameNodeButton").on("click", edit.renameNode);
		}
	};

	console.log("Module ready: renameNodePg.js");
	return _;
}();

// # sourceURL=renameNodePg.js
