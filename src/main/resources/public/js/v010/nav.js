console.log("running module: nav.js");

var nav = function() {
	/*
	 * ================= PUBLIC =================
	 */
	var _ = {

		navScrollTop : function() {
			scrollToTop();
		},

		displayingRoot : function() {
			return meta64.js.currentNodeId === meta64.js.homeNodeId;
		},

		navUpLevel : function() {
			console.log("==========navUpLevel.");
			if (_.displayingRoot()) {
				// alert("Already at root. Can't go up.");
				return;
			}

			util.json("renderNode", {
				"nodeId" : meta64.js.currentNodeId,
				"upLevel" : 1
			}, view.renderNodeResponse);
		},

		/*
		 * turn of row selection NodeInfo.java obj of whatever row is currently
		 * selected
		 */
		getFocusedNode : function() {

			/*
			 * check if we have an existing highlighted row to unhighlight, and
			 * is done by looking up current parent node for the page
			 */
			var currentSelNode = meta64.js.parentUidToFocusNodeMap[meta64.js.currentNodeUid];

			if (currentSelNode) {
				/* get node by node identifier */
				var node = meta64.js.uidToNodeMap[currentSelNode.uid];
				return node;
			}
			return null;
		},

		/*
		 * turn of row selection DOM element of whatever row is currently
		 * selected
		 */
		getSelectedDomElement : function() {

			/*
			 * check if we have an existing highlighted row to unhighlight, and
			 * is done by looking up current parent node for the page
			 */
			var currentSelNode = meta64.js.parentUidToFocusNodeMap[meta64.js.currentNodeUid];
			if (currentSelNode) {
				console.log("Unhighlighting previous row: currentNodeId=" + meta64.js.currentNodeUid);

				/* get node by node identifier */
				var node = meta64.js.uidToNodeMap[currentSelNode.uid];

				if (node) {
					// console.log("found highlighted node.id=" + node.id);

					/* now make CSS id from node */
					var nodeId = node.uid + "_row";
					// console.log("looking up using element id: "+nodeId);

					return util.domElm(nodeId);
				}
			}
			return null;
		},

		/*
		 * turn of row selection styling of whatever row is currently selected
		 */
		unhighlightRow : function() {

			console.log("unhighlight row");
			var currentUid = meta64.js.currentNodeUid;
			console.log("  currentParentUid=" + currentUid);

			/* check if we have an existing highlighted row to unhighlight */
			var currentSelNode = meta64.js.parentUidToFocusNodeMap[currentUid];
			if (!currentSelNode.uid) {
				alert("oops, unhighlight says current node has null uid");
			}
			
			if (currentSelNode) {
				console.log("Unhighlighting previous row: currentNodeUid=" + currentSelNode.uid + ", path: "
						+ meta64.getPathOfUid(currentSelNode.uid));

				/* get node by node identifier */
				var node = meta64.js.uidToNodeMap[currentSelNode.uid];

				if (node) {
					console.log("    found highlighted node.uid=" + node.uid);

					/* now make CSS id from node */
					var nodeId = node.uid + "_row";
					console.log("    looking up using element id: " + nodeId);

					var elm = util.domElm(nodeId);
					if (elm) {
						/* change class on element */
						util.changeOrAddClass(elm, "active-row", "inactive-row");
					} else {
						alert('oops.');
						console.log("ERR: unable to find row element with id: " + nodeId);
						console.trace();
					}
				} else {
					console.log("ERR: failed to find uidToNodeMap item for uid:" + currentSelNode.uid);
					console.trace();
				}
			} else {
				console.log("  no parent node found for: currentUid=" + currentUid);
			}
		},

		clickOnNodeRow : function(rowElm, uid) {

			_.unhighlightRow();

			var node = meta64.js.uidToNodeMap[uid];
			if (!node) {
				console.log("clickOnNodeRow recieved uid that doesn't map to any node. uid=" + uid);
			}

			/*
			 * sets which node is selected on this page (i.e. parent node of
			 * this page being the 'key')
			 */
			meta64.js.parentUidToFocusNodeMap[meta64.js.currentNodeUid] = node;
			if (!node.uid) {
				alert("oops, node.uid is null");
			}

			util.changeOrAddClass(rowElm, "inactive-row", "active-row");
		},

		openNode : function(uid) {
			var node = meta64.js.uidToNodeMap[uid];
			if (!node) {
				alert("Unknown nodeId in openNode: " + uid);
			} else {
				view.refreshTree(node.id);
			}
		},

		toggleNodeSel : function(uid) {
			var checked = util.getRequiredElement('#' + uid + "_sel").is(":checked");
			if (checked) {
				meta64.js.selectedNodes[uid] = true;
			} else {
				delete meta64.js.selectedNodes[uid];
			}
			view.updateStatusBar();
			meta64.refreshAllGuiEnablement();
			// alert("selections: " + printKeys(selectedNodes));
		},

		navHome : function() {
			util.json("renderNode", {
				"nodeId" : meta64.js.homeNodeId
			}, view.renderNodeResponse);
		}
	};

	console.log("Module ready: nav.js");
	return _;
}();
