console.log("running module: attachment.js");

var attachment = function() {

	function _deleteAttachmentResponse(res) {
		if (util.checkSuccess("Delete attachment", res)) {
			/*
			 * All that's needed to update client is to set the binary flag on
			 * the node to false, and re-render
			 */
			_.uploadNode.hasBinary = false;
			render.renderPageFromData();
			_.closeUploadDialog();
		}
	}

	var _ = {

		/* Node being uploaded to */
		uploadNode : null,

		deleteAttachment : function() {
			util.areYouSure("Confirm Delete Attachment", "Delete the Attachment on the Node?", "Yes, delete.", function() {
				util.json("deleteAttachment", {
					"nodeId" : _.uploadNode.id,
				}, _deleteAttachmentResponse);
			});
		},

		uploadFileNow : function() {

			/* Upload form has hidden input element for nodeId parameter */
			$("#uploadFormNodeId").attr("value", _.uploadNode.id);

			/*
			 * This is the only place we do something differently from the
			 * normal 'util.json()' calls to the server, because this is highly
			 * specialized here for form uploading, and is different from normal
			 * ajax calls.
			 */
			$.ajax({
				url : postTargetUrl + "upload",
				type : "POST",
				data : new FormData($("#uploadForm")[0]),
				enctype : 'multipart/form-data',
				processData : false,
				contentType : false,
				cache : false,
				success : function() {
					view.refreshTree();
					_.closeUploadDialog();
				},
				error : function() {
					alert("Upload failed.");
				}
			});
		},

		populateUploadDialog : function() {

			/* display the node path at the top of the edit page */
			$("#uploadPathDisplay").html("Path: " + render.formatPath(_.uploadNode.path));
		},

		openUploadDialog : function(uid) {
			var node = meta64.uidToNodeMap[uid];
			if (!node) {
				_.uploadNode = null;
				alert("Unknown nodeId in upload: " + uid);
				return;
			}

			_.uploadNode = node;
			_.populateUploadDialog();
			$.mobile.changePage("#uploadDialog");
		},

		closeUploadDialog : function() {
			$.mobile.changePage("#mainPage");
			view.scrollToSelectedNode();
		}
	};

	console.log("Module ready: attachment.js");
	return _;
}();
