console.log("running module: searchResultsPg.js");

var searchResultsPg = function() {

	var _ = {
		build : function() {

			var header = render.makeTag("div", //
			{
				"data-role" : "header" //
			}, //
			"<h2>" + BRANDING_TITLE + " - Search Results</h2>");

			var backToContentButton = render.makeBackButton("Back to Content", "cancelSearchResultsButton", "b");
			var buttonBar = render.makeHorzControlGroup(backToContentButton);

			var internalMainContent = "<div id='searchResultsView'></div>";
			var mainContent = render.makeTag("div", //
			{
				"role" : "main", //
				"class" : "ui-content"
			}, //
			buttonBar + internalMainContent);

			var content = header + mainContent;
			util.setHtmlEnhanced($("#searchResultsPg"), content);
		},
		
		init : function() {
			srch.populateSearchResultsPage();
		}
	};

	console.log("Module ready: searchResultsPg.js");
	return _;
}();

// # sourceURL=searchResultsPg.js
