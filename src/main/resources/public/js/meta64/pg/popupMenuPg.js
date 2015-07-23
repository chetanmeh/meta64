console.log("running module: popupMenuPg.js");

var popupMenuPg = function() {

	function _makeTopLevelMenu(title, content) {
		return render.makeTag("li", {
			"data-role" : "collapsible",
			"data-iconpos" : "right",
			"data-shadow" : "false",
			"data-corners" : "false"
		}, "<h2>" + title + "</h2>" + //
		_makeSecondLevelList(content));
	}

	function _makeSecondLevelList(content) {
		return render.makeTag("ul", {
			"data-role" : "listview",
			"data-inset" : "true",
			"data-shadow" : "false",
			"data-corners" : "false"
		}, content);
	}

	function _menuItem(name, id, onClick) {
		var anchor = render.makeTag("a", {
			"id" : id,
			"onclick" : onClick
		}, name);
		return "<li>" + anchor + "</li>";
	}

	var _ = {
		build : function() {

			var myAccountItems = _menuItem("Change Password", "changePasswordPgButton", "user.changePasswordPg();") + //
			_menuItem("Preferences", "accountPreferencesPgButton", "prefs.accountPreferencesPg();") + //
			_menuItem("Insert Book: War and Peace", "insertBookWarAndPeaceButton", " edit.insertBookWarAndPeace();") + //
			_menuItem("Donate", "donatePgButton", "meta64.openDonatePg();");
			var myAccountMenu = _makeTopLevelMenu("My Account", myAccountItems);

			var editMenuItems = _menuItem("Attachments", "manageAttachmentsButton", "attachment.openUploadPgMenuClick();") + // 
			_menuItem("Sharing", "editNodeSharingButton", "share.editNodeSharingMenuClick();") + // 
			_menuItem("Move", "moveSelNodesButton", "edit.moveSelNodes();") + // 
			_menuItem("Finish Moving", "finishMovingSelNodesButton", "edit.finishMovingSelNodes();") + // 
			_menuItem("Export", "openExportPgButton", "edit.openExportPg();") + // 
			_menuItem("Import", "openImportPgButton", "edit.openImportPg();") + // 
			_menuItem("Delete", "deleteSelNodesButton", "edit.deleteSelNodes();");// 
			var editMenu = _makeTopLevelMenu("Edit", editMenuItems);

			var searchMenuItems = _menuItem("Text Search", "searchPgButton", "srch.searchPg();") + // 
			_menuItem("Timeline", "timelineButton", "srch.timeline();");// 
			var searchMenu = _makeTopLevelMenu("Search", searchMenuItems);

			var content = render.makeTag("ul", {
				"data-role" : "listview",
				"style" : "min-width: 300px;"
			}, myAccountMenu + editMenu + searchMenu);

			util.setHtmlEnhanced($("#popupMenuPg"), content);
		},

		init : function() {
			
			var selNodeCount = util.getPropertyCount(meta64.selectedNodes);
			var highlightNode = meta64.getHighlightedNode();
			var propsToggle = meta64.currentNode && !meta64.isAnonUser;
			var editMode = meta64.currentNode && !meta64.isAnonUser;
			var canFinishMoving = !util.nullOrUndef(edit.nodesToMove) && !_.isAnonUser;
			
			util.setEnablement($("#changePasswordPgButton"), !meta64.isAnonUser);
			util.setEnablement($("#accountPreferencesPgButton"), !meta64.isAnonUser);
			util.setEnablement($("#insertBookWarAndPeaceButton"), meta64.isAdminUser && highlightNode != null);
			util.setEnablement($("#donatePgButton"), true);			
			util.setEnablement($("#manageAttachmentsButton"), highlightNode != null); 
			util.setEnablement($("#editNodeSharingButton"), highlightNode != null);
			util.setEnablement($("#moveSelNodesButton"), highlightNode != null);
			util.setEnablement($("#finishMovingSelNodesButton"), canFinishMoving); 
			util.setEnablement($("#openExportPgButton"), meta64.isAdminUser);
			util.setEnablement($("#openImportPgButton"), meta64.isAdminUser); 
			util.setEnablement($("#deleteSelNodesButton"), selNodeCount); 
			util.setEnablement($("#searchPgButton"), highlightNode != null);
			util.setEnablement($("#timelineButton"), highlightNode != null); 
		}
	};

	console.log("Module ready: popupMenuPg.js");
	return _;
}();

// # sourceURL=popupMenuPg.js
