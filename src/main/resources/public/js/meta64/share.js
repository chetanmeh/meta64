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
		_.populateSharingPg(res);
	}

	var _renderAclPrivileges = function(principal, aclEntry) {
		var ret = "";
		$.each(aclEntry.privileges, function(index, privilege) {

			var removeButton = render.makeTag("a", //
			{
				"onClick" : "share.removePrivilege('" + principal + "', '" + privilege.privilegeName + "');", //
				"class" : "ui-btn ui-btn-inline ui-icon-delete ui-btn-icon-left"
			}, //
			"Remove");

			var row = render.makeHorizontalFieldSet(removeButton);

			row += "<b>" + principal + "</b> has privilege <b>" + privilege.privilegeName 
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
			"includeAcl" : true,
			"includeOwners" : true
		}, _getNodePrivilegesResponse);
	}

	var _ = {

		sharingNode : null,

		populateSharingPg : function(res) {

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
		},
		
		removePrivilege : function(principal, privilege) {
			util.json("removePrivilege", {
				"nodeId" : _.sharingNode.id,
				"principal" : principal,
				"privilege" : privilege
			}, _removePrivilegeResponse);
		},

		shareNodeToPersonPg : function() {
			meta64.changePage("#shareToPersonPg");
		},
		
		shareNodeToPerson : function() {
			var targetUser = $("#shareToUserName").val(); 
			if (!targetUser) {
				alert("Please enter a username");
				return;
			}
			
			/* TODO: Need to test of 'nodeTypeManagement' is actually required. What is below does work, but need
			 * to make it as minimal as possible
			 */
			util.json("addPrivilege", {
				"nodeId" : _.sharingNode.id,
				"principal" : targetUser,
				"privileges" : ["read", "write", "addChildren", "nodeTypeManagement"]
			}, _.reloadFromShareWithPerson);
		},
		
		shareNodeToPublic : function() {
			console.log("Sharing node to public.");
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
				"privileges" : ["read"]
			}, _.reload);
		},

		/*
		 * Handles 'Sharing' button on a specific node, from button bar above
		 * node display in edit mode
		 */
		editNodeSharingMenuClick : function() {
			var node = meta64.getHighlightedNode();

			if (!node) {
				alert("No node is selected.");
				return;
			}
			_.sharingNode = node;
			meta64.changePage("#sharingPg");
		},

		reloadFromShareWithPerson : function(res) {
			if (util.checkSuccess("Share Node with Person", res)) {
				meta64.changePage("#sharingPg");
			}
		},
		
		/*
		 * Gets privileges from server and displays in GUI also. Assumes gui is already
		 * at correct page.
		 */
		reload : function() {
			console.log("Loading node sharing info.");
			
			util.json("getNodePrivileges", {
				"nodeId" : _.sharingNode.id,
				"includeAcl" : true,
				"includeOwners" : true
			}, _getNodePrivilegesResponse);
		}
	};

	console.log("Module ready: share.js");
	return _;
}();

//# sourceURL=share.js