console.log("running module: attachment.js");

var attachment = function() {

	function _deleteAttachmentResponse(res) {
		if (util.checkSuccess("Delete attachment", res)) {
			// /*
			// * All that's needed to update client is to set the binary flag on
			// * the node to false, and re-render
			// */
			// _.uploadNode.hasBinary = false;
			// render.renderPageFromData();

			/*
			 * noticed the above is broken so for now let's just refresh the
			 * page,but the above USED to work, simply by setting hasBinary to
			 * false. So something's changed and that no longer works.
			 */
			view.refreshTree(null, false);

			_.closeUploadPg();
		}
	}
	
	function _uploadFromUrlResponse(res) {
		if (util.checkSuccess("Upload from URL", res)) {
			view.refreshTree(null, false);
			_.closeUploadPg();
		}
	}

	var _ = {

		/* Node being uploaded to */
		uploadNode : null,

		deleteAttachment : function() {
			confirmPg.areYouSure("Confirm Delete Attachment", "Delete the Attachment on the Node?", "Yes, delete.", function() {
				util.json("deleteAttachment", {
					"nodeId" : _.uploadNode.id
				}, _deleteAttachmentResponse);
			});
		},

		uploadFileNow : function() {

			var sourceUrl = $("#uploadFromUrl").val();

			/* if uploading from URL */
			if (sourceUrl) {
				util.json("uploadFromUrl", {
					"nodeId" : _.uploadNode.id,
					"sourceUrl" : sourceUrl
				}, _uploadFromUrlResponse);
			}
			/* Else uploading from local computer */
			else {

				/* Upload form has hidden input element for nodeId parameter */
				$("#uploadFormNodeId").attr("value", _.uploadNode.id);

				/*
				 * This is the only place we do something differently from the
				 * normal 'util.json()' calls to the server, because this is
				 * highly specialized here for form uploading, and is different
				 * from normal ajax calls.
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
						view.refreshTree(null, false);
						_.closeUploadPg();
					},
					error : function() {
						alert("Upload failed.");
					}
				});
			}
		},

		openUploadPgMenuClick : function() {
			var node = meta64.getHighlightedNode();

			if (!node) {
				_.uploadNode = null;
				alert("No node is selected.");
				return;
			}

			_.uploadNode = node;
			meta64.changePage("#uploadPg");
		},

		closeUploadPg : function() {
			meta64.changePage("#mainPage");
			view.scrollToSelectedNode();
		}
	};

	console.log("Module ready: attachment.js");
	return _;
}();

//# sourceURL=attachment.js
