console.log("running module: uploadPg.js");

var uploadPg = function() {

	var _ = {
		build : function() {

			var header = render.makeTag("div", //
			{
				"data-role" : "header" //
			}, //
			"<h2>" + BRANDING_TITLE + " - Upload File Attachment</h2>");

			var uploadPathDisplay = render.makeTag("div", {//
				"id" : "uploadPathDisplay",
				"class" : "path-display-in-editor"
			}, "");

			var formFields = render.makeTag("input", {
				"type" : "file",
				"name" : "file"
			}, "", true) + //
			render.makeTag("input", {
				"id" : "uploadFormNodeId",
				"type" : "hidden",
				"name" : "nodeId"
			})

			var form = render.makeTag("form", {
				"id" : "uploadForm",
				"method" : "POST",
				"enctype" : "multipart/form-data"
			}, formFields);

			var uploadFieldContainer = render.makeTag("div", {//
				"id" : "uploadFieldContainer"
			}, "<p>Upload from your computer</p>" + form);

			var divider = "<div><h3>-- OR --</h3></div>";

			var uploadFromUrlField = render.makeEditField("Upload From URL", "uploadFromUrl");
			var uploadFromUrlDiv = render.makeTag("div", {//
			}, uploadFromUrlField);

			var uploadButton = render.makeButton("Upload", "uploadButton", "b", "ui-btn-icon-left ui-icon-check");
			var deleteButton = render.makeButton("Delete", "deleteAttachmentButton", "a");
			var backButton = render.makeBackButton("Close", "closeUploadButton", "a");
			var buttonBar = render.makeHorzControlGroup(uploadButton + deleteButton + backButton);

			var mainContent = render.makeTag("div", //
			{
				"role" : "main", //
				"class" : "ui-content"
			}, //
			uploadPathDisplay + uploadFieldContainer + divider + uploadFromUrlDiv + buttonBar);

			util.setHtmlEnhanced($("#uploadPg"), header + mainContent);

			$("#uploadButton").on("click", attachment.uploadFileNow);
			$("#closeUploadButton").on("click", attachment.closeUploadPg);
			$("#deleteAttachmentButton").on("click", attachment.deleteAttachment);
		},
		
		init : function() {

			$("#uploadFromUrl").val("");
			
			/* display the node path at the top of the edit page */
			$("#uploadPathDisplay").html("Path: " + render.formatPath(attachment.uploadNode.path));
		}
	};

	console.log("Module ready: uploadPg.js");
	return _;
}();

// # sourceURL=uploadPg.js
