console.log("running module: search.js");

function clickSearchNode(uid) {
	/*
	 * update highlight node to point to the node clicked on, just to persist it
	 * for later
	 */
	search.highlightRowNode = search.uidToNodeMap[uid];
	view.refreshTree(search.highlightRowNode.id);
	$.mobile.changePage("#");
};

var search = function() {

	var _UID_ROWID_SUFFIX = "_srch_row";

	/*
	 * ================= PRIVATE =================
	 */

	var _searchNodesResponse = function(res) {
		_.renderSearchResultsFromData(res);
	};

	/*
	 * ================= PUBLIC =================
	 */
	var _ = {
		clickSearchNode2 : function(uid) {
			/*
			 * update highlight node to point to the node clicked on, just to
			 * persist it for later
			 */
			search.highlightRowNode = search.uidToNodeMap[uid];
			view.refreshTree(search.highlightRowNode.id);
			$.mobile.changePage("#");
		},

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
		},

		/*
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
				"onClick" : "search.clickOnSearchResultRow(this, '" + uid + "');", //
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
				/* For some VERY strange reason this call doesn't work. Says the function is not existing. WRONG Chrome.
				 * This is either an exceedly strange browser bug or the scope named 'search' is overriding this only 
				 * in the anchor tag. If I use the exact same 'search.clickSearchNode2' in the onClick of a DIV tag
				 * it works perfectly well so the functio DOES exist and DOES have the correct name here. I spent 
				 * SEVERAL HOURS trying to figure out what is choking the onclick method here, with no progress, other 
				 * than to discover a globally scoped function WILL work.
				 */
				//"onClick" : "search.clickSearchNode2('" + uid + "');", //
				
				//This version works, oddly only because of global scope. VERY strange. I want to use
				//search scope but that fails. See note above.
				"onClick" : "clickSearchNode('" + uid + "');", //
				
				"data-role" : "button",
				"data-icon" : "plus",
				"data-theme" : "b"
			}, //
			"Open");

			return render.makeHorizontalFieldSet(openButton);
		},

		clickOnSearchResultRow : function(rowElm, uid) {
			verifyFunction();

			_.unhighlightRow();
			_.highlightRowNode = _.uidToNodeMap[uid];

			util.changeOrAddClass(rowElm, "inactive-row", "active-row");
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

function verifyFunction() {
	if (typeof clickSearchNode != 'function') {
		console.log("************************************ Failed creating clickSearchNode function");
	} else {
		console.log("************************************ clickSearchNode is STILL a function.");
	}
}

verifyFunction();
