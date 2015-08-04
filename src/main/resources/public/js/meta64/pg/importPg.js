console.log("running module: importPg.js");

var importPg = function() {

	var _ = {
			domId : "importPg",
			
		build : function() {

			var header = render.makeTag("div", //
			{
				"data-role" : "header"//,
				//"data-position" : "fixed",
				//"data-tap-toggle" : "false"
			}, //
			"<h2>" + BRANDING_TITLE + " - Import from XML</h2>");

			var formControls = render.makeEditField("Import Target Node Name", "importTargetNodeName");

			var importButton = render.makeButton("Import", "importNodesButton", "b");
			var backButton = render.makeBackButton("Close", "cancelImportButton", "a");
			var buttonBar = render.makeHorzControlGroup(importButton + backButton);

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
			util.setHtmlEnhanced($("#importPg"), content);

			$("#importNodesButton").on("click", edit.importNodes);
		}
	};

	console.log("Module ready: importPg.js");
	return _;
}();

// # sourceURL=importPg.js
