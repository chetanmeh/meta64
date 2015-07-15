console.log("running module: view.js");

var view = function() {

	var _ = {
		updateStatusBar : function() {
			if (!meta64.currentNodeData)
				return;
			var statusLine = "";

			if (meta64.editModeOption === meta64.MODE_ADVANCED) {
				statusLine += "count: " + meta64.currentNodeData.children.length;
			}

			if (meta64.editMode) {
				statusLine += " Selections: " + util.getPropertyCount(meta64.selectedNodes);
			}

			var visible = statusLine.length > 0;
			util.setVisibility("#mainNodeStatusBar", visible);

			if (visible) {
				util.getRequiredElement("#mainNodeStatusBar").html(statusLine);
			}
		},

		refreshTree : function(nodeId, renderParentIfLeaf) {
			if (!nodeId) {
				nodeId = meta64.currentNodeId;
			}

			util.json("renderNode", {
				"nodeId" : nodeId,
				"renderParentIfLeaf" : renderParentIfLeaf ? true : false
			}, _.renderNodeResponse, {
				"nodeRenderedId" : nodeId
			});
		},

		/*
		 * data is instanceof RenderNodeResponse.java
		 */
		renderNodeResponse : function(data, info) {
			// console.log("renderNode: " + JSON.stringify(data));
			render.renderPageFromData(data);
			
			if (info && data.displayedParent) {
				
				//this sets the focused node using id
				var node = meta64.getNodeFromId(info.nodeRenderedId);
				meta64.parentUidToFocusNodeMap[meta64.currentNodeUid] = node;
				_.scrollToSelectedNode();
				nav.simulateClickOnNodeRow(node.uid);
			}
			else {
				util.scrollToTop();
			}
			
			meta64.refreshAllGuiEnablement();
		},

		scrollToSelectedNode : function() {
			setTimeout(function() {
				var elm = nav.getSelectedDomElement();
				if (elm) {
					var node = nav.getFocusedNode();
					var ordinal = meta64.getOrdinalOfNode(node);

					/*
					 * set scroll position to exact top (zero) if this is the
					 * first (top) child node, or else scroll exactly to it
					 */
					var scrollPos = (ordinal == 0 ? 0 : $('#' + elm.id).offset().top - $('#mainPageHeader').height());

					// console.log("Scrolling to dom element id: " +elm.id);
					$('html, body').animate({
						scrollTop : scrollPos
					});
				}
			}, 1000);
		},

		initEditPathDisplayById : function(domId) {
			var node = edit.editNode;
			var e = $(domId);
			if (edit.editingUnsavedNode) {
				e.hide();
			} else {
				e.html("Path: " + render.formatPath(node.path))
				e.show();
			}
		}
	};

	console.log("Module ready: view.js");
	return _;
}();
