console.log("running module: sharingPg.js");

var sharingPg = function() {

	var _ = {
			domId : "sharingPg",
			
		build : function() {

			var header = render.makeTag("div", //
			{
				"data-role" : "header"//,
				//"data-position" : "fixed"
				//"data-tap-toggle" : "false"
			}, //
			"<h2>" + BRANDING_TITLE + " - Node Sharing</h2>");

			var shareWithPersonButton = render.makeButton("Share with Person", "shareNodeToPersonPgButton", "a");
			var makePublicButton = render.makeButton("Share to Public", "shareNodeToPublicButton", "a");
			var backButton = render.makeBackButton("Close", "cancelSharingButton", "a");
			var buttonBar = render.makeHorzControlGroup(shareWithPersonButton + makePublicButton + backButton);

			var internalMainContent = "<div id='shareNodeNameDisplay'></div>" + //
			"<div id='sharingListFieldContainer'></div>";
			
			var mainContent = render.makeTag("div", //
			{
				"role" : "main", //
				"class" : "ui-content"
			}, //
			internalMainContent + buttonBar);

			var content = header + mainContent;
			util.setHtmlEnhanced($("#sharingPg"), content);

			$("#shareNodeToPersonPgButton").on("click", share.shareNodeToPersonPg);
			$("#shareNodeToPublicButton").on("click", share.shareNodeToPublic);
		},
		
		init : function() {
			share.reload();
		}
	};

	console.log("Module ready: sharingPg.js");
	return _;
}();

// # sourceURL=sharingPg.js
