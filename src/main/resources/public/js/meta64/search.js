console.log("running module: search.js");

/*
 * WARNING
 * 
 * Due to something apparently in the global namespace, if I try to use 'search'
 * as the variable name here instead of 'srch' the onClick handler of JQuery
 * Mobile-decorated anchor tags gives an error saying the function is not found.
 * So either Chrome or JQuery is somehow not compatible with a global variable
 * named 'search'.
 */

var srch = function() {

	var _UID_ROWID_SUFFIX = "_srch_row";

	var _searchNodesResponse = function(res) {
		_.searchResults = res;

		_.renderSearchResultsFromData(res);
	};

	var _ = {

		/*
		 * Holds the NodeSearchResponse.java JSON, or null if no search has been
		 * done.
		 */
		searchResults : null,

		/*
		 * Will be the last row clicked on (NodeInfo.java object) and having the
		 * red highlight bar
		 */
		highlightRowNode : null,

		/*
		 * maps node 'identifier' (assigned at server) to uid value which is a
		 * value based off local sequence, and uses nextUid as the counter.
		 */
		identToUidMap : {},

		/*
		 * maps node.uid values to the NodeInfo.java objects
		 * 
		 * The only contract about uid values is that they are unique insofar as
		 * any one of them always maps to the same node. Limited lifetime
		 * however. The server is simply numbering nodes sequentially. Actually
		 * represents the 'instance' of a model object. Very similar to a
		 * 'hashCode' on Java objects.
		 */
		uidToNodeMap : {},

		numSearchResults : function() {
			return srch.searchResults != null && //
			srch.searchResults.searchResults != null && //
			srch.searchResults.searchResults.length != null ? //
			srch.searchResults.searchResults.length : 0;
		},

		searchNodes : function() {
			var node = nav.getFocusedNode();
			if (!node) {
				alert("No node is selected to search under.");
				return;
			}

			var searchText = $.trim($("#searchText").val());
			if (util.emptyString(searchText)) {
				alert("Enter search text.");
				return;
			}

			util.json("nodeSearch", {
				"nodeId" : node.id,
				"searchText" : searchText
			}, _searchNodesResponse);
		},

		searchNodesDialog : function() {
			$.mobile.changePage("#searchNodesDialog");
		},

		initSearchNode : function(node) {
			node.uid = util.getUidForId(_.identToUidMap, node.id);
			// node.properties =
			// props.setPreferredPropertyOrder(node.properties);

			_.uidToNodeMap[node.uid] = node;
		},

		renderSearchResultsFromData : function(data) {

			var output = '';
			var childCount = data.searchResults.length;

			/*
			 * Number of rows that have actually made it onto the page to far.
			 * Note: some nodes get filtered out on the client side for various
			 * reasons.
			 */
			var rowCount = 0;

			$.each(data.searchResults, function(i, node) {
				if (meta64.isNodeBlackListed(node))
					return;

				_.initSearchNode(node);

				rowCount++;
				output += _.renderSearchResultAsListItem(node, i, childCount, rowCount);
			});

			util.setHtmlEnhanced($("#searchResultsView"), output);
			$.mobile.changePage("#searchResultsDialog");
		},

		/*
		 * Renders a single line of search results on the search results page.
		 * 
		 * node is a NodeInfo.java JSON
		 */
		renderSearchResultAsListItem : function(node, index, count, rowCount) {

			var uid = node.uid;

			/*
			 * this checking of "rep:" is just a hack for now to stop from
			 * deleting things I won't want to allow to delete, but I will
			 * design this better later.
			 */
			var isRep = node.name.startsWith("rep:") || meta64.currentNodeData.node.path.contains("/rep:");
			var editingAllowed = meta64.isAdminUser || !isRep;

			// /*
			// * if not selected by being the new child, then we try to select
			// * based on if this node was the last one clicked on for this
			// page.
			// */
			// // console.log("test: [" + parentIdToFocusIdMap[currentNodeId]
			// // +"]==["+ node.id + "]")
			// var focusNode =
			// meta64.parentUidToFocusNodeMap[meta64.currentNodeUid];
			// if (!selected && focusNode && focusNode.uid === uid) {
			// selected = true;
			// }

			var cssId = uid + _UID_ROWID_SUFFIX;
			// console.log("Rendering Node Row[" + index + "] with id: " +cssId)
			return render.makeTag("div", //
			{
				"class" : "node-table-row inactive-row",
				"onClick" : "srch.clickOnSearchResultRow(this, '" + uid + "');", //
				"id" : cssId
			},// 
			_.makeButtonBarHtml(uid) + render.makeTag("div", //
			{
				"id" : uid + "_srch_content"
			}, render.renderNodeContent(node, true, true, true, true)));
		},

		makeButtonBarHtml : function(uid) {

			var openButton = render.makeTag("a", //
			{
				"onClick" : "srch.clickSearchNode('" + uid + "');", //

				"data-role" : "button",
				"data-icon" : "plus",
				"data-theme" : "b"
			}, //
			"Open");

			return render.makeHorizontalFieldSet(openButton);
		},

		clickOnSearchResultRow : function(rowElm, uid) {
			//verifyFunction();

			_.unhighlightRow();
			_.highlightRowNode = _.uidToNodeMap[uid];

			util.changeOrAddClass(rowElm, "inactive-row", "active-row");
		},

		clickSearchNode : function(uid) {
			/*
			 * update highlight node to point to the node clicked on, just to
			 * persist it for later
			 */
			srch.highlightRowNode = srch.uidToNodeMap[uid];
			view.refreshTree(srch.highlightRowNode.id);
			$.mobile.changePage("#");
		},

		/*
		 * turn of row selection styling of whatever row is currently selected
		 */
		unhighlightRow : function() {

			if (!_.highlightRowNode) {
				return;
			}

			/* now make CSS id from node */
			var nodeId = _.highlightRowNode.uid + _UID_ROWID_SUFFIX;

			var elm = util.domElm(nodeId);
			if (elm) {
				/* change class on element */
				util.changeOrAddClass(elm, "active-row", "inactive-row");
			}
		}
	};

	console.log("Module ready: search.js");
	return _;
}();
