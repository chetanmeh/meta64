console.log("running module: user.js");

var share = function() {

	/*
	 * Handles getNodePrivileges respons.
	 * 
	 * res=json of GetNodePrivilegesResponse.java
	 * 
	 * res.aclEntries = list of AccessControlEntryInfo.java json objects
	 */
	var _getNodePrivilegesResponse = function(res) {
		_populateSharingDialog(res);

		$.mobile.changePage("#shareNodeDialog");
	}

	var _populateSharingDialog = function(res) {

		var html = "<h2>Access Control Entries</h2>";

		$.each(res.aclEntries, function(index, aclEntry) {
			html += "<h4>User: " + aclEntry.principalName + "</h4>";
			html += render.makeTag("div", {
				"class" : "privilege-list"
			}, _renderAclPrivileges(aclEntry.principalName, aclEntry));

		});

		if (html === "") {
			html = "Node is not shared with anyone.";
		}

		util.setHtmlEnhanced($("#sharingListFieldContainer"), html);
	}

	var _renderAclPrivileges = function(principal, aclEntry) {
		var ret = "";
		$.each(aclEntry.privileges, function(index, privilege) {

			var removeButton = render.makeTag("a", //
			{
				"onClick" : "share.removePrivilege('" + principal + "', '" + privilege.privilegeName + "');", //
				"data-role" : "button",
				"data-icon" : "delete"
			}, //
			"Remove");

			var row = render.makeHorizontalFieldSet(removeButton);

			row += "<b>" + principal + "</b> has privilege <b>" + privilege.privilegeName /* _makeFriendlyPrivilegeName(privilege.privilegeName) */
					+ "</b> on this node.";

			ret += render.makeTag("div", {
				"class" : "privilege-entry"
			}, row);
		});
		return ret;
	}

	var _removePrivilegeResponse = function(res) {
		util.json("getNodePrivileges", {
			"nodeId" : _.sharingNode.path,
			"includeAcl" : "y",
			"includeOwners" : "y"
		}, _getNodePrivilegesResponse);
	}

	// var _makeFriendlyPrivilegeName = function(privName) {
	// if (privName === "jcr:read") {
	// return "read";
	// } else {
	// return privName;
	// }
	// }

	var _ = {

		sharingNode : null,

		removePrivilege : function(principal, privilege) {
			util.json("removePrivilege", {
				"nodeId" : _.sharingNode.id,
				"principal" : principal,
				"privilege" : privilege
			}, _removePrivilegeResponse);
		},

		shareNodeToPublic : function() {

			/*
			 * Add privilege and then reload share nodes dialog from scratch
			 * doing another callback to server
			 * 
			 * TODO: this additional call can be avoided as an optimization some
			 * day
			 */
			util.json("addPrivilege", {
				"nodeId" : _.sharingNode.id,
				"principal" : "everyone",
				"privilege" : "read"
			}, _.reload);
		},

		/*
		 * Handles 'Sharing' button on a specific node, from button bar above
		 * node display in edit mode
		 */
		editNodeSharingMenuClick : function() {
			var node = nav.getHighlightedNode();

			if (!node) {
				alert("No node is selected.");
				return;
			}
			_.sharingNode = node;
			_.reload();
		},

		reload : function() {
			util.json("getNodePrivileges", {
				"nodeId" : _.sharingNode.id,
				"includeAcl" : "y",
				"includeOwners" : "y"
			}, _getNodePrivilegesResponse);
		}
	};

	console.log("Module ready: share.js");
	return _;
}();