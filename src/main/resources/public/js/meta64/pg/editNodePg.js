console.log("running module: editNodePg.js");

var editNodePg = function() {

	var _ = {
		domId : "editNodePg",

		build : function() {

			var header = render.makeTag("div", //
			{
				"data-role" : "header"// ,
			// "data-position" : "fixed",
			// "data-tap-toggle" : "false"
			}, //
			"<h2>" + BRANDING_TITLE + " - Edit Node</h2>");

			var saveNodeButton = render.makeButton("Save", "saveNodeButton", "b", "ui-btn-icon-left ui-icon-check");
			var addPropertyButton = render.makeButton("Add Property", "addPropertyButton", "a");
			var makeNodeReferencableButton = render.makeButton("Make Node Referencable", "makeNodeReferencableButton", "a");
			var cancelEditButton = render.makeButton("Close", "cancelEditButton", "a");
			var buttonBar = render.makeHorzControlGroup(saveNodeButton + addPropertyButton + makeNodeReferencableButton + cancelEditButton);

			var internalMainContent = "<div id='editNodePathDisplay' class='path-display-in-editor'></div>" + //
			"<div id='editNodeInstructions'></div>" + //
			"<div id='propertyEditFieldContainer'></div>";

			var mainContent = render.makeTag("div", //
			{
				"role" : "main", //
				"class" : "ui-content dialog-content"
			}, //
			internalMainContent + buttonBar);

			var content = header + mainContent;

			util.setHtmlEnhanced($("#editNodePg"), content);

			$("#saveNodeButton").on("click", edit.saveNode);
			$("#cancelEditButton").on("click", edit.cancelEdit);
			$("#addPropertyButton").on("click", props.addProperty);
			$("#makeNodeReferencableButton").on("click", edit.makeNodeReferencable);
		},

		init : function() {
			edit.populateEditNodePg();
		}
	};

	console.log("Module ready: editNodePg.js");
	return _;
}();

// # sourceURL=editNodePg.js
