console.log("running module: search.js");

var search = function() {

	/*
	 * ================= PRIVATE =================
	 */

	var _searchNodesResponse = function(res) {
		_.renderSearchResultsFromData(res);
	}

	/*
	 * ================= PUBLIC =================
	 */
	var _ = {

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

			var cssId = uid + "_srch_row";
			// console.log("Rendering Node Row[" + index + "] with id: " +cssId)
			return render.makeTag("div", //
			{
				"class" : "node-table-row inactive-row",
				"onClick" : "search.clickOnSearchResultRow(this, '" + uid + "');", //
				"id" : cssId
			},// 
			/* _.makeButtonBarHtml(uid, canMoveUp, canMoveDown, editingAllowed) + */render.makeTag("div", //
			{
				"id" : uid + "_srch_content"
			}, render.renderNodeContent(node, true, true, true, true)));
		},

		clickOnSearchResultRow : function(rowElm, uid) {

			_.unhighlightRow();

			// var node = _.uidToNodeMap[uid];
			// if (!node) {
			// console.log("clickOnNodeRow recieved uid that doesn't map to any
			// node. uid=" + uid);
			// return;
			// }

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
			var nodeId = _.highlightRowNode.uid + "_srch_row";

			var elm = util.domElm(nodeId);
			if (elm) {
				/* change class on element */
				util.changeOrAddClass(elm, "active-row", "inactive-row");
			}
		}
	}

	console.log("Module ready: search.js");
	return _;
}();