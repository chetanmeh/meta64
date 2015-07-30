console.log("running module: jcrCnst.js");

var jcrCnst = function() {

	var _ = {
		MIXIN_TYPES : "jcr:mixinTypes",

		EMAIL_CONTENT : "jcr:content",
		EMAIL_RECIP : "recip",
		EMAIL_SUBJECT : "subject",

		CREATED : "jcr:created",
		CREATED_BY : "jcr:createdBy",
		CONTENT : "jcr:content",
		UUID : "jcr:uuid",
		LAST_MODIFIED : "jcr:lastModified",
		LAST_MODIFIED_BY : "jcr:lastModifiedBy",

		USER : "user",
		PWD : "pwd",
		EMAIL : "email",
		CODE : "code",

		BIN_VER : "binVer",
		BIN_DATA : "jcrData",
		BIN_MIME : "jcr:mimeType",

		IMG_WIDTH : "imgWidth",
		IMG_HEIGHT : "imgHeight"
	};

	console.log("Module ready: jcrCnst.js");
	return _;
}();

// # sourceURL=jcrCnst.js
