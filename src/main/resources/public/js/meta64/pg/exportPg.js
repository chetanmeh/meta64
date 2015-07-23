console.log("running module: exportPg.js");

var exportPg = function() {

	var _ = {
		build : function() {

			var header = render.makeTag("div", //
			{
				"data-role" : "header" //
			}, //
			"<h2>" + BRANDING_TITLE + " - Export to XML</h2>");

			var formControls = render.makeEditField("Export to File Name", "exportTargetNodeName");

			var exportButton = render.makeButton("Export", "exportNodesButton", "b");
			var backButton = render.makeBackButton("Close", "cancelExportButton", "a");
			var buttonBar = render.makeHorzControlGroup(exportButton + backButton);

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
			util.setHtmlEnhanced($("#exportPg"), content);

			$("#exportNodesButton").on("click", edit.exportNodes);
		}
	};

	console.log("Module ready: exportPg.js");
	return _;
}();

// # sourceURL=exortPg.js
