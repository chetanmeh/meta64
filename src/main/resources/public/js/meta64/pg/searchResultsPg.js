console.log("running module: searchResultsPg.js");

var searchResultsPg = function() {

	var _ = {
		build : function() {

			var header = render.makeTag("div", //
			{
				"data-role" : "header" //
			}, //
			render.makeButton("Back to Content", "cancelSearchResultsButton", "b") + //
			"<h2>" + BRANDING_TITLE + " - Search Results</h2>");

			var internalMainContent = "<div id='searchResultsView'></div>";
			var mainContent = render.makeTag("div", //
			{
				"role" : "main", //
				"class" : "ui-content"
			}, //
			internalMainContent);

			var content = header + mainContent;
			util.setHtmlEnhanced($("#searchResultsPg"), content);

			$("#cancelSearchResultsButton").on("click", function() {
				$.mobile.changePage("#mainPage");
				view.scrollToSelectedNode();
			});
		},

		init : function() {
			srch.populateSearchResultsPage();
		}
	};

	console.log("Module ready: searchResultsPg.js");
	return _;
}();

// # sourceURL=searchResultsPg.js
