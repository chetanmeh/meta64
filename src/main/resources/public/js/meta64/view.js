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

		refreshTreeResponse : function(res, targetId, renderParentIfLeaf) {
			render.renderPageFromData(res);
			if (targetId && renderParentIfLeaf && res.displayedParent) {
				meta64.highlightRowById(targetId, true);
			} else {
				_.scrollToSelectedNode();
			}
			meta64.refreshAllGuiEnablement();
		},

		refreshTree : function(nodeId, renderParentIfLeaf) {
			if (!nodeId) {
				nodeId = meta64.currentNodeId;
			}

			var prms = util.json("renderNode", {
				"nodeId" : nodeId,
				"renderParentIfLeaf" : renderParentIfLeaf ? true : false
			});

			prms.done(function(res) {
				_.refreshTreeResponse(res, nodeId, renderParentIfLeaf);
			});
		},

		scrollToSelectedNode : function() {
			setTimeout(function() {
				var elm = nav.getSelectedDomElement();
				if (elm) {
					var node = meta64.getHighlightedNode();
					var ordinal = meta64.getOrdinalOfNode(node);

					/*
					 * set scroll position to exact top (zero) if this is the
					 * first (top) child node, or else scroll exactly to it
					 */
					var scrollPos = (ordinal == 0 ? 0 : $('#' + elm.id).offset().top - $('#mainPageHeader').height());
					// console.log("scrollPos: "+scrollPos);

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
				e.html("Path: " + render.formatPath(node))
				e.show();
			}
		}
	};

	console.log("Module ready: view.js");
	return _;
}();

// # sourceURL=view.js
