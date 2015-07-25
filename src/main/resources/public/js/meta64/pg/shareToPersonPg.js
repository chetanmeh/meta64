console.log("running module: shareToPersonPg.js");

var shareToPersonPg = function() {

	var _ = {
		build : function() {

			var header = render.makeTag("div", //
			{
				"data-role" : "header" //
			}, //
			"<h2>" + BRANDING_TITLE + " - Share Node to Person</h2>");

			var formControls = render.makeEditField("User to Share With", "shareToUserName");

			var shareButton = render.makeButton("Share", "shareNodeToPersonButton", "b", "ui-btn-icon-left ui-icon-check");
			var backButton = render.makeBackButton("Close", "cancelShareNodeToPersonButton", "a");
			var buttonBar = render.makeHorzControlGroup(shareButton + backButton);

			var form = render.makeTag("div", //
			{
				"class" : "ui-field-contain" //
			}, //
			formControls + buttonBar);

			var internalMainContent = "Enter the username of the person you want to share this node with:";
			var mainContent = render.makeTag("div", //
			{
				"role" : "main", //
				"class" : "ui-content"
			}, //
			internalMainContent + form);

			var content = header + mainContent;
			util.setHtmlEnhanced($("#shareToPersonPg"), content);

			$("#shareNodeToPersonButton").on("click", share.shareNodeToPerson);
		}
	};

	console.log("Module ready: shareToPersonPg.js");
	return _;
}();

// # sourceURL=shareToPersonPg.js
