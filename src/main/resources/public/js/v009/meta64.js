console.log("running module: meta64.js");

var meta64 = function() {
	/*
	 * TODO: move to meta64 file.
	 * 
	 * We have to be able to map any identifier to a uid, that will be
	 * repeatable, so we have to use a local 'hashset-type' implementation
	 */
	var _getUidForId = function(ident) {
		/* look for uid in map */
		var uid = meta64.js.identToUidMap[ident];

		/* if not found, get next number, and add to map */
		if (!uid) {
			uid = meta64.js.nextUid++;
			meta64.js.identToUidMap[ident] = uid;
		}
		return uid;
	}

	/*
	 * ================= PUBLIC =================
	 */
	var _ = {
		js : {
			/*
			 * User's root node. Top level of what logged in user is allowed to
			 * see.
			 */
			homeNodeId : "",
			homeNodePath : "",

			/*
			 * specifies if this is admin user. Server side still protects
			 * itself from all access, even if this variable is hacked by
			 * attackers.
			 */
			isAdminUser : false,

			/*
			 * signals that data has changed and the next time we go to the main
			 * tree view window we need to refresh data from the server
			 */
			treeDirty : false,

			/*
			 * maps node.uid values to the NodeInfo.java objects
			 * 
			 * The only contract about uid values is that they are unique
			 * insofar as any one of them always maps to the same node. Limited
			 * lifetime however. The server is simply numbering nodes
			 * sequentially. Actually represents the 'instance' of a model
			 * object. Very similar to a 'hashCode' on Java objects.
			 */
			uidToNodeMap : {},

			/* counter for local uids */
			nextUid : 0,

			/*
			 * maps node 'identifier' (assigned at server) to uid value which is
			 * a value based off local sequence, and uses nextUid as the
			 * counter.
			 */
			identToUidMap : {},

			/*
			 * maps action name values to the action objects. Action objects
			 * have properties: "name", "enable", etc...
			 */
			actionNameToObjMap : {},

			/*
			 * Under any given node, there can be one active 'selected' node
			 * that has the highlighting, and will be scrolled to whenever the
			 * page with that child is visited, and this object holds the map of
			 * parent uid to selected node (NodeInfo object), where the key is
			 * the parent node uid, and the value is the currently selected node
			 * within that parent. Note this 'selection state' is only
			 * significant on the client, and only for being able to scroll to
			 * the node during navigating around on the tree.
			 */
			parentUidToFocusNodeMap : {},

			/*
			 * determines if we should render all the editing buttons on each
			 * row
			 * 
			 * warning, doesn't currently support defaulting to true right here.
			 */
			editMode : false,

			MODE_ADVANCED : "advanced",
			MODE_SIMPLE : "simple",

			/* can be 'simple' or 'advanced' */
			editModeOption : "simple",

			/*
			 * toggled by button, and holds if we are going to show properties
			 * or not on each node in the main view
			 */
			showProperties : false,

			/*
			 * List of node prefixes to flag nodes to not allow to be shown in
			 * the page in simple mode
			 */
			simpleModeNodePrefixBlackList : {
				"rep:" : true
			},

			simpleModePropertyBlackList : {
				"jcr:primaryType" : true,
				"rep:policy" : true
			},

			readOnlyPropertyList : {
				"jcr:uuid" : true,
				"jcr:mixinTypes" : true
			},

			binaryPropertyList : {
				"jcr:data" : true
			},

			/*
			 * Property fields are generated dynamically and this maps the DOM
			 * IDs of each field to the property object it edits.
			 */
			fieldIdToPropMap : {},

			/*
			 * maps all node uids to true if selected, otherwise the property
			 * should be deleted (not existing)
			 */
			selectedNodes : {},

			/* identifier of newly created node */
			newChildNodeId : "",

			/* RenderNodeResponse.java object */
			currentNodeData : null,

			/*
			 * all variables derivable from currentNodeData, but stored directly
			 * for simpler code/access
			 */
			currentNode : null,
			currentNodeUid : null,
			currentNodeId : null,
			currentNodePath : null
		},

		inSimpleMode : function() {
			return _.js.editModeOption === _.js.MODE_SIMPLE;
		},

		isNodeBlackListed : function(node) {
			if (!_.inSimpleMode())
				return false;

			var prop;
			for (prop in _.js.simpleModeNodePrefixBlackList) {
				if (_.js.simpleModeNodePrefixBlackList.hasOwnProperty(prop) && node.name.startsWith(prop)) {
					return true;
				}
			}

			return false;
		},

		getPathOfUid : function(uid) {
			var node = _.js.uidToNodeMap[uid];
			if (!node) {
				return "[path error. invalid uid: " + uid + "]";
			} else {
				return node.path;
			}
		},

		/*
		 * All action function names must end with 'Action', and are prefixed by
		 * the action name.
		 */
		defineAllActions : function() {
			var displayingNode = !util.emptyString(_.js.currentNode);

			/*
			 * Define all actions and enablement for them.
			 * 
			 * IMPORTANT: Each one of the 'name' values below must have a DOM id
			 * associated with it that is like [name]Button (i.e. suffixed with
			 * 'Button'). Example: id='loginButton'
			 */
			_.defineActions({
				"name" : "tryAnotherCaptcha",
				"enable" : true,
				"function" : user.tryAnotherCaptcha
			}, {
				"name" : "login",
				"enable" : true,
				"function" : user.login
			}, {
				"name" : "navHome",
				"enable" : displayingNode && !nav.displayingRoot(),
				"function" : nav.navHome
			}, {
				"name" : "navUpLevel",
				"enable" : displayingNode && !nav.displayingRoot(),
				"function" : nav.navUpLevel
			}, {
				"name" : "propsToggle",
				"enable" : displayingNode,
				"function" : props.propsToggle
			}, {
				"name" : "saveNode",
				"enable" : false,
				"function" : edit.saveNode
			}, {
				"name" : "cancelEdit",
				"enable" : false,
				"function" : edit.cancelEdit
			}, {
				"name" : "addProperty",
				"enable" : false,
				"function" : props.addProperty
			}, {
				"name" : "deleteProperty",
				"enable" : false,
				"function" : props.deleteProperty
			}, {
				"name" : "saveProperty",
				"enable" : false,
				"function" : props.saveProperty
			}, {
				"name" : "changePasswordDialog",
				"enable" : false,
				"function" : user.changePasswordDialog
			}, {
				"name" : "changePassword",
				"enable" : false,
				"function" : user.changePassword
			}, {
				"name" : "editMode",
				"enable" : displayingNode,
				"function" : edit.editMode
			}, {
				"name" : "signup",
				"enable" : true,
				"function" : user.signup
			}, {
				"name" : "accountPreferencesDialog",
				"enable" : true,
				"function" : prefs.accountPreferencesDialog
			}, {
				"name" : "savePreferences",
				"enable" : true,
				"function" : prefs.savePreferences
			}, {
				"name" : "shareNodeToPublic",
				"enable" : true,
				"function" : share.shareNodeToPublic
			}, {
				"name" : "deleteAttachment",
				"enable" : true,
				"function" : attachment.deleteAttachment
			}, {
				"name" : "makeNodeReferencable",
				"enable" : true,
				"function" : edit.makeNodeReferencable
			}, {
				"name" : "insertBookWarAndPeace",
				"enable" : true,
				"function" : edit.insertBookWarAndPeace
			});
			
			// hookSliderChanges("editMode");
		},

		refreshAllGuiEnablement : function() {
			/* multiple select nodes */
			var selNodeCount = util.getPropertyCount(_.js.selectedNodes);

			util.setEnablementByName("login", true);
			util.setEnablementByName("navHome", _.js.currentNode && !nav.displayingRoot());
			util.setEnablementByName("navUpLevel", _.js.currentNode && !nav.displayingRoot());
			util.setEnablementByName("propsToggle", _.js.currentNode);
			util.setEnablementByName("saveNode", true);
			util.setEnablementByName("cancelEdit", true);
			util.setEnablementByName("addProperty", true);
			util.setEnablementByName("deleteProperty", true);
			util.setEnablementByName("saveProperty", true);
			util.setEnablementByName("changePassword", true);
			util.setEnablementByName("changePasswordDialog", true);
			util.setEnablementByName("editMode", _.js.currentNode);
			util.setEnablementByName("signup", true);
			util.setEnablementByName("insertBookWarAndPeace", _.js.isAdminUser, _.js.isAdminUser);
		},

		/*
		 * Naming convention, example "doSomething"
		 * 
		 * Action Name: doSomething Button Element ID: doSomethingButton
		 * Function handling it: doSomethingAction
		 * 
		 * And hooks a click function to each id.
		 */
		defineActions : function(actions) {
			for (var i = 0; i < arguments.length; i++) {
				var action = arguments[i];
				var actionName = action["name"];
				var func = action["function"];

				_.js.actionNameToObjMap[actionName] = action;

				if (typeof func !== "function") {
					console.log("Function not found for action " + actionName);
					continue;
				}

				var id = "#" + actionName + "Button";
				if (!util.hookClick(id, func)) {
					console.log("Failed to hook button: " + actionName);
					return;
				}

				var elm = $(id);
				if (elm) {
					util.setEnablement(elm, action["enable"]);
				} else {
					console.log("Unable to set enablement. ID not found: " + id);
				}
			}
		},

		getSingleSelectedNode : function() {
			var uid;
			for (uid in _.js.selectedNodes) {
				if (_.js.selectedNodes.hasOwnProperty(uid)) {
					// console.log("found a single Sel NodeID: " + nodeId);
					var singleSelNode = _.js.uidToNodeMap[uid];
					// if (singleSelNode == null) {
					// console.log("id doesn't map to a node.");
					// } else {
					// console.log("singleSelId: " +
					// singleSelNode.id);
					// }
					return singleSelNode;
				}
			}
			return null;
		},

		/* node = NodeInfo.java object */
		getOrdinalOfNode : function(node) {
			if (!_.js.currentNodeData || !_.js.currentNodeData.children)
				return -1;

			for (var i = 0; i < _.js.currentNodeData.children.length; i++) {
				if (node.id === _.js.currentNodeData.children[i].id) {
					return i;
				}
			}
			return -1;
		},

		setCurrentNodeData : function(data) {
			_.js.currentNodeData = data;
			_.js.currentNode = data.node;
			_.js.currentNodeUid = data.node.uid;
			_.js.currentNodeId = data.node.id;
			_.js.currentNodePath = data.node.path;
		},

		hookInitFunction : function() {
			/*
			 * JQM docs says do the 'pagecreate' thing instead of
			 * $(document).ready()
			 * 
			 * Warning: If you leave off the second parameter it calls this for
			 * each page load, which can hook buttons multiple times, etc.,
			 * which is a major malfunction, so I target the specific page
			 * "#mainPage" so that it can only call this ONE time.
			 */
			// $(document).ready(function() {
			$(document).on("pagecreate", "#mainPage", function(event) {
				_.initApp();
			});
		},

		anonPageLoadResponse : function(res) {
			if (res.renderNodeResponse) {
				util.setVisibility("#mainNodeContent", true);
				util.setVisibility("#mainNodeStatusBar", true);
				view.renderNodeResponse(res.renderNodeResponse);
			} else {
				util.setVisibility("#mainNodeContent", false);
				util.setVisibility("#mainNodeStatusBar", false);

				util.setHtmlEnhanced($("#listView"), res.content);
			}
		},

		/*
		 * updates client side maps and client-side identifier for new node, so
		 * that this node is 'recognized' by client side code
		 */
		initNode : function(node) {
			node.uid = _getUidForId(node.id);
			node.properties = props.setPreferredPropertyOrder(node.properties);

			// console.log("******* initNode uid=" + node.uid);
			meta64.js.uidToNodeMap[node.uid] = node;
		},

		initApp : function() {
			console.log("initApp running.");

			_.defineAllActions();

			util.json("anonPageLoad", {}, _.anonPageLoadResponse);
		}
	};

	/*
	 * TODO: I need to understand this 'on' event better, I never fully
	 * researched it...
	 */
	$(document).on("pagebeforechange", function(e, data) {
		var toPage = data.toPage[0].id;
		console.log("Nav to page: " + toPage);

		if (toPage == "signupDialog") {
			user.pageInitSignupDialog();
			// $.mobile.pageContainer.pagecontainer("change", "#pageZ");
		}
		// this doesn't execute ???? so I'm just updating enablement
		// very time a
		// selection changes.
		// else if (toPage == "popupMenu") {
		// console.log("popup showing now.");
		// refreshAllGuiEnablement();
		// }
	});

	console.log("Module ready: meta64.js");
	return _;
}();
