console.log("running module: nav.js");

var nav = function() {
	var _UID_ROWID_SUFFIX = "_row";

	var _ = {

		displayingHome : function() {
			if (meta64.isAnonUser) {
				return meta64.currentNodeId === meta64.anonUserLandingPageNode;
			} else {
				return meta64.currentNodeId === meta64.homeNodeId;
			}
		},

		parentVisibleToUser : function() {
			return !_.displayingHome();
		},

		showSearchPage : function() {
			$.mobile.changePage("#searchResultsPg");
		},

		upLevelResponse : function(res, info) {
			if (!res || !res.node) {
				alert("No data is visible to you above this node.");
				util.setEnablementByName("navUpLevel", false);
			} else {
				render.renderPageFromData(res);
				meta64.highlightRowById(info.id, true);
				meta64.refreshAllGuiEnablement();
			}
		},

		navUpLevel : function() {

			if (!_.parentVisibleToUser()) {
				// alert("Already at root. Can't go up.");
				return;
			}

			util.json("renderNode", {
				"nodeId" : meta64.currentNodeId,
				"upLevel" : 1
			}, _.upLevelResponse, {
				"id" : meta64.currentNodeId
			});
		},

		/*
		 * turn of row selection DOM element of whatever row is currently
		 * selected
		 */
		getSelectedDomElement : function() {

			var currentSelNode = meta64.getHighlightedNode();
			if (currentSelNode) {

				/* get node by node identifier */
				var node = meta64.uidToNodeMap[currentSelNode.uid];

				if (node) {
					// console.log("found highlighted node.id=" + node.id);

					/* now make CSS id from node */
					var nodeId = node.uid + _UID_ROWID_SUFFIX;
					// console.log("looking up using element id: "+nodeId);

					return util.domElm(nodeId);
				}
			}

			return null;
		},

		clickOnNodeRow : function(rowElm, uid) {

			//moving this logic inside meta64.highlightNode
			//_.unhighlightRow();

			var node = meta64.uidToNodeMap[uid];
			if (!node) {
				console.log("clickOnNodeRow recieved uid that doesn't map to any node. uid=" + uid);
				return;
			}

			/*
			 * sets which node is selected on this page (i.e. parent node of
			 * this page being the 'key')
			 */
			meta64.highlightNode(node, false);

			if (meta64.editMode) {

				/*
				 * if node.owner is currently null, that means we have not
				 * retrieve the owner from the server yet, but if non-null it's
				 * already displaying and we do nothing.
				 */
				if (!node.owner) {
					meta64.updateNodeInfo(node);
				}
			}
		},

		openNode : function(uid) {

			var node = meta64.uidToNodeMap[uid];
			
			meta64.highlightNode(node, true);

			if (!node) {
				alert("Unknown nodeId in openNode: " + uid);
			} else {
				view.refreshTree(node.id, false);
			}
		},

		toggleNodeSel : function(uid) {
			var btn = util.getRequiredElement();
			if (!btn) {
				console.log("Unable to find Sel button for uid: "+uid);
				return;
			}
			
			var elm = $('#' + uid + "_sel");
			var classes = elm.attr("class");
			
			var checked = classes.contains("ui-btn-b");
			
			if (checked) {
				util.changeOrAddClass(elm, "ui-btn-b", "ui-btn-a");
				checked = false;
			}
			else {
				util.changeOrAddClass(elm, "ui-btn-a", "ui-btn-b");
				checked = true;
			}
			
			console.log("Classes: "+classes);
			
			if (checked) {
				meta64.selectedNodes[uid] = true;
			} else {
				delete meta64.selectedNodes[uid];
			}
			elm.enhanceWithin();
			view.updateStatusBar();
			meta64.refreshAllGuiEnablement();
			// alert("selections: " + printKeys(selectedNodes));
		},

		navHomeResponse : function(res) {
			meta64.clearSelectedNodes();
			render.renderPageFromData(res);
			util.scrollToTop();
			meta64.refreshAllGuiEnablement();
		},

		navHome : function() {
			if (meta64.isAnonUser) {
				meta64.loadAnonPageHome(true);
				// window.location.href = window.location.origin;
			} else {
				util.json("renderNode", {
					"nodeId" : meta64.homeNodeId
				}, _.navHomeResponse);
			}
		}
	};

	console.log("Module ready: nav.js");
	return _;
}();

//# sourceURL=nav.js
