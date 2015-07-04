console.log("running module: attachment.js");

var attachment = function() {

	/*
	 * ================= PRIVATE =================
	 */

	function _deleteAttachmentResponse(res) {
		if (res.success) {
			/*
			 * All that's needed to update client is to set the binary flag on
			 * the node to false, and re-render
			 */
			_.js.uploadNode.hasBinary = false;
			render.renderPageFromData();
			_.closeUploadDialog();
		} else {
			alert("Unable to delete attachment.");
		}
	}

	/*
	 * ================= PUBLIC =================
	 */
	var _ = {
		js : {
			/* Node being uploaded to */
			uploadNode : null
		},

		deleteAttachment : function() {
			util.areYouSure("Confirm Delete Attachment", "Delete the Attachment on the Node?", "Yes, delete.", function() {
				util.json("deleteAttachment", {
					"nodeId" : _.js.uploadNode.id,
				}, _deleteAttachmentResponse);
			});
		},

		uploadFileNow : function() {

			/* Upload form has hidden input element for nodeId parameter */
			$("#uploadFormNodeId").attr("value", _.js.uploadNode.id);

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
			$("#uploadPathDisplay").html("Path: " + render.formatPath(_.js.uploadNode.path));
		},

		openUploadDialog : function(uid) {
			var node = meta64.js.uidToNodeMap[uid];
			if (!node) {
				_.js.uploadNode = null;
				alert("Unknown nodeId in upload: " + uid);
				return;
			}

			_.js.uploadNode = node;
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
