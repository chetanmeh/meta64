console.log("running module: view.js");

var view = function() {
	/*
	 * ================= PUBLIC =================
	 */
	var _ = {
		updateStatusBar : function() {
			if (!meta64.js.currentNodeData)
				return;
			var statusLine = "";

			if (meta64.js.editModeOption === meta64.js.MODE_ADVANCED) {
				statusLine += "count: " + meta64.js.currentNodeData.children.length;
			}

			if (meta64.js.editMode) {
				statusLine += " Selections: " + util.getPropertyCount(meta64.js.selectedNodes);
			}

			var visible = statusLine.length > 0;
			util.setVisibility("#mainNodeStatusBar", visible);
			
			if (visible) {
				util.getRequiredElement("#mainNodeStatusBar").html(statusLine);
			} 
		},

		refreshTree : function(nodeId) {
			if (!nodeId) {
				nodeId = meta64.js.currentNodeId;
			}

			util.json("renderNode", {
				"nodeId" : nodeId,
			}, _.renderNodeResponse);
		},

		/*
		 * data is instanceof RenderNodeResponse.java
		 */
		renderNodeResponse : function(data) {
			console.log("renderNode: " + JSON.stringify(data));
			render.renderPageFromData(data);
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
			var node = edit.js.editNode;
			var e = $(domId);
			if (edit.js.editingUnsavedNode) {
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
