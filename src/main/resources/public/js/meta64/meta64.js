console.log("running module: meta64.js");

var meta64 = function() {

	var appInitialized = false;
	var curUrlPath = window.location.pathname + window.location.search;

	var _ = {

		userName : "anonymous",
		deviceWidth : 0,
		deviceHeight : 0,

		/*
		 * User's root node. Top level of what logged in user is allowed to see.
		 */
		homeNodeId : "",
		homeNodePath : "",

		/*
		 * specifies if this is admin user. Server side still protects itself
		 * from all access, even if this variable is hacked by attackers.
		 */
		isAdminUser : false,

		/* always start out as anon user until login */
		isAnonUser : true,
		anonUserLandingPageNode : null,

		/*
		 * signals that data has changed and the next time we go to the main
		 * tree view window we need to refresh data from the server
		 */
		treeDirty : false,

		/*
		 * maps node.uid values to the NodeInfo.java objects
		 * 
		 * The only contract about uid values is that they are unique insofar as
		 * any one of them always maps to the same node. Limited lifetime
		 * however. The server is simply numbering nodes sequentially. Actually
		 * represents the 'instance' of a model object. Very similar to a
		 * 'hashCode' on Java objects.
		 */
		uidToNodeMap : {},

		/*
		 * maps node.id values to NodeInfo.java objects
		 */
		idToNodeMap : {},

		/* counter for local uids */
		nextUid : 1,

		/*
		 * maps node 'identifier' (assigned at server) to uid value which is a
		 * value based off local sequence, and uses nextUid as the counter.
		 */
		identToUidMap : {},

		/*
		 * maps action name values to the action objects. Action objects have
		 * properties: "name", "enable", etc...
		 */
		actionNameToObjMap : {},

		/*
		 * Under any given node, there can be one active 'selected' node that
		 * has the highlighting, and will be scrolled to whenever the page with
		 * that child is visited, and this object holds the map of parent uid to
		 * selected node (NodeInfo object), where the key is the parent node
		 * uid, and the value is the currently selected node within that parent.
		 * Note this 'selection state' is only significant on the client, and
		 * only for being able to scroll to the node during navigating around on
		 * the tree.
		 */
		parentUidToFocusNodeMap : {},

		/*
		 * determines if we should render all the editing buttons on each row
		 */
		editMode : false,

		// TODO: move these to cnst.js.
		MODE_ADVANCED : "advanced",
		MODE_SIMPLE : "simple",

		/* can be 'simple' or 'advanced' */
		editModeOption : "simple",

		/*
		 * toggled by button, and holds if we are going to show properties or
		 * not on each node in the main view
		 */
		showProperties : false,

		/*
		 * List of node prefixes to flag nodes to not allow to be shown in the
		 * page in simple mode
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
			"jcr:mixinTypes" : true,
			"jcr:created" : true,
			"jcr:createdBy" : true,
			"jcr:lastModified" : true,
			"jcr:lastModifiedBy" : true,
			"imgWidth" : true,
			"imgHeight" : true,
			"binVer" : true,
			"jcr:mimeType" : true
		},

		binaryPropertyList : {
			"jcr:data" : true
		},

		/*
		 * Property fields are generated dynamically and this maps the DOM IDs
		 * of each field to the property object it edits.
		 */
		fieldIdToPropMap : {},

		/*
		 * maps all node uids to true if selected, otherwise the property should
		 * be deleted (not existing)
		 */
		selectedNodes : {},

		/* identifier of newly created node */
		newChildNodeId : "",

		/* RenderNodeResponse.java object */
		currentNodeData : null,

		/*
		 * all variables derivable from currentNodeData, but stored directly for
		 * simpler code/access
		 */
		currentNode : null,
		currentNodeUid : null,
		currentNodeId : null,
		currentNodePath : null,

		inSimpleMode : function() {
			return _.editModeOption === _.MODE_SIMPLE;
		},

		changePage : function(pageId) {
			pageMgr.buildPage(pageId);
			$.mobile.pageContainer.pagecontainer("change", pageId);
		},

		popup : function(pageId) {
			pageMgr.buildPage(pageId);
			$(pageId).popup("open");
		},

		isNodeBlackListed : function(node) {
			if (!_.inSimpleMode())
				return false;

			var prop;
			for (prop in _.simpleModeNodePrefixBlackList) {
				if (_.simpleModeNodePrefixBlackList.hasOwnProperty(prop) && node.name.startsWith(prop)) {
					return true;
				}
			}

			return false;
		},

		getSelectedNodeUidsArray : function() {
			var selArray = [];
			var idx = 0;
			var uid;
			for (uid in _.selectedNodes) {
				if (_.selectedNodes.hasOwnProperty(uid)) {
					selArray[idx++] = uid;
				}
			}
			return selArray;
		},

		getSelectedNodeIdsArray : function() {
			var selArray = [];
			var idx = 0;
			var uid;
			if (!_.selectedNodes) {
				console.log("no selected nodes.");
			} else {
				console.log("selectedNode count: " + _.selectedNodes.length);
			}
			for (uid in _.selectedNodes) {
				if (_.selectedNodes.hasOwnProperty(uid)) {
					var node = _.uidToNodeMap[uid];
					if (!node) {
						console.log("unable to find uidToNodeMap for uid=" + uid);
					} else {
						selArray[idx++] = node.id;
					}
				}
			}
			return selArray;
		},

		/* Gets selected nodes as NodeInfo.java objects array */
		getSelectedNodesArray : function() {
			var selArray = [];
			var idx = 0;
			var uid;
			for (uid in _.selectedNodes) {
				if (_.selectedNodes.hasOwnProperty(uid)) {
					selArray[idx++] = _.uidToNodeMap[uid];
				}
			}
			return selArray;
		},

		clearSelectedNodes : function() {
			_.selectedNodes = {};
		},

		updateNodeInfoResponse : function(res, info) {
			var ownerBuf = '';
			// console.log("res: "+ JSON.stringify(res));
			var mine = false;
			$.each(res.owners, function(index, owner) {
				if (ownerBuf.length > 0) {
					ownerBuf += ",";
				}

				if (owner === meta64.userName) {
					mine = true;
				}

				ownerBuf += owner;
				// console.log("ownerbuf: "+ownerBuf);
			});

			if (ownerBuf.length > 0) {
				info.node.owner = ownerBuf;
				var elm = $("#ownerDisplay" + info.node.uid);
				elm.html(" (Manager: " + ownerBuf + ")");
				if (mine) {
					util.changeOrAddClass(elm, "created-by-other", "created-by-me");
				} else {
					util.changeOrAddClass(elm, "created-by-me", "created-by-other");
				}
			}
		},

		updateNodeInfo : function(node) {
			util.json("getNodePrivileges", {
				"nodeId" : node.id,
				"includeAcl" : "n",
				"includeOwners" : "y"
			}, _.updateNodeInfoResponse, {
				"node" : node
			});
		},

		/* Returns the node with the given node.id value */
		getNodeFromId : function(id) {
			return _.idToNodeMap[id];
		},

		getPathOfUid : function(uid) {
			var node = _.uidToNodeMap[uid];
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
			var displayingNode = !util.emptyString(_.currentNode);

			/*
			 * Define all actions and enablement for them.
			 * 
			 * IMPORTANT: Each one of the 'name' values below must have a DOM id
			 * associated with it that is like [name]Button (i.e. suffixed with
			 * 'Button'). Example: id='loginButton'
			 */
			_.defineActions({
				"name" : "openLoginPg",
				"enable" : true,
				"function" : user.openLoginPg
			}, {
				"name" : "navHome",
				"enable" : displayingNode && !nav.displayingHome(),
				"function" : nav.navHome
			}, {
				"name" : "navUpLevel",
				"enable" : displayingNode && nav.parentVisibleToUser(),
				"function" : nav.navUpLevel
			}, {
				"name" : "propsToggle",
				"enable" : displayingNode,
				"function" : props.propsToggle
			}, {
				"name" : "deleteProperty",
				"enable" : false,
				"function" : props.deleteProperty
			}, {
				"name" : "editMode",
				"enable" : displayingNode,
				"function" : edit.editMode
			}, {
				"name" : "makeNodeReferencable",
				"enable" : true,
				"function" : edit.makeNodeReferencable
			});
		},

		openDonatePg : function() {
			meta64.changePage("#donatePg");
		},

		getHighlightedNode : function() {
			// console.log("getHighlightedNode looking up: " +
			// _.currentNodeUid);
			var ret = _.parentUidToFocusNodeMap[_.currentNodeUid];
			// console.log(" found it: " + (ret ? true : false));
			return ret;
		},

		highlightRowById : function(id, scroll) {
			var node = _.getNodeFromId(id);
			if (node) {
				_.highlightNode(node, scroll);
			} else {
				console.log("highlightRowById failed to find id: " + id);
			}
		},

		/*
		 * Important: We want this to be the only method that can set values on
		 * 'parentUidToFocusNodeMap', and always setting that value should go
		 * thru this function.
		 */
		highlightNode : function(node, scroll) {
			if (!node)
				return;

			var doneHighlighting = false;

			/* Unhighlight currently highlighted node if any */
			var curHighlightedNode = _.parentUidToFocusNodeMap[_.currentNodeUid];
			if (curHighlightedNode) {
				if (curHighlightedNode.uid === node.uid) {
					// console.log("already highlighted.");
					doneHighlighting = true;
				} else {
					var rowElmId = curHighlightedNode.uid + "_row";
					var rowElm = $("#" + rowElmId);
					util.changeOrAddClass(rowElm, "active-row", "inactive-row");
				}
			}

			if (!doneHighlighting) {
				_.parentUidToFocusNodeMap[_.currentNodeUid] = node;

				var rowElmId = node.uid + "_row";
				var rowElm = $("#" + rowElmId);
				util.changeOrAddClass(rowElm, "inactive-row", "active-row");
			}

			if (scroll) {
				view.scrollToSelectedNode();
			}
		},

		refreshAllGuiEnablement : function() {
			/* multiple select nodes */
			var selNodeCount = util.getPropertyCount(_.selectedNodes);
			var highlightNode = _.getHighlightedNode();

			util.setEnablementByName("navHome", _.currentNode && !nav.displayingHome());
			util.setEnablementByName("navUpLevel", _.currentNode && nav.parentVisibleToUser());

			var propsToggle = _.currentNode && !_.isAnonUser;
			/*
			 * this leaves a hole in the toolbar if you hide it. Need to change
			 * that
			 */
			util.setEnablementByName("propsToggle", propsToggle);

			util.setEnablementByName("deleteProperty", !_.isAnonUser);

			var editMode = _.currentNode && !_.isAnonUser;
			// console.log(">>>>>>>>>>>>>>> currentNode=" + _.currentNode + "
			// anonUser=" + _.anonUser);
			/*
			 * this leaves a hole in the toolbar if you hide it. Need to change
			 * that
			 */
			util.setEnablementByName("editMode", editMode);

			util.setVisibility("#menuButton", !_.isAnonUser);
			util.setVisibility("#mainMenuSearchButton", !_.isAnonUser && highlightNode != null);
			util.setVisibility("#mainMenuTimelineButton", !_.isAnonUser && highlightNode != null);
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

				_.actionNameToObjMap[actionName] = action;

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
			for (uid in _.selectedNodes) {
				if (_.selectedNodes.hasOwnProperty(uid)) {
					// console.log("found a single Sel NodeID: " + nodeId);
					var singleSelNode = _.uidToNodeMap[uid];
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
			if (!_.currentNodeData || !_.currentNodeData.children)
				return -1;

			for (var i = 0; i < _.currentNodeData.children.length; i++) {
				if (node.id === _.currentNodeData.children[i].id) {
					return i;
				}
			}
			return -1;
		},

		setCurrentNodeData : function(data) {
			_.currentNodeData = data;
			_.currentNode = data.node;
			_.currentNodeUid = data.node.uid;
			_.currentNodeId = data.node.id;
			_.currentNodePath = data.node.path;
		},

		// hookInitFunction : function() {
		// /*
		// * JQM docs says do the 'pagecreate' thing instead of
		// * $(document).ready()
		// *
		// * Warning: If you leave off the second parameter it calls this for
		// * each page load, which can hook buttons multiple times, etc.,
		// * which is a major malfunction, so I target the specific page
		// * "#mainPage" so that it can only call this ONE time.
		// */
		// // $(document).ready(function() {
		// // _.loadAnonPageHome(false);
		// // };
		//			
		// $(document).on("pagecreate", "#mainPage", function(event) {
		// // _.initApp();
		// });
		// },

		anonPageLoadResponse : function(res) {
			if (res.renderNodeResponse) {

				util.setVisibility("#mainNodeContent", true);
				util.setVisibility("#mainNodeStatusBar", true);

				render.renderPageFromData(res.renderNodeResponse);
				_.refreshAllGuiEnablement();
			} else {
				util.setVisibility("#mainNodeContent", false);
				util.setVisibility("#mainNodeStatusBar", false);

				console.log("setting listview to: " + res.content);
				util.setHtmlEnhanced($("#listView"), res.content);
			}
			render.renderMainPageControls();
		},

		/*
		 * updates client side maps and client-side identifier for new node, so
		 * that this node is 'recognized' by client side code
		 */
		initNode : function(node) {
			if (!node) {
				console.log("initNode has null node");
				return;
			}
			/*
			 * assign a property for detecting this node type, I'll do this
			 * instead of using some kind of custom JS prototype-related
			 * approach
			 */
			node.uid = util.getUidForId(_.identToUidMap, node.id);
			node.properties = props.setPreferredPropertyOrder(node.properties);

			// console.log("******* initNode uid=" + node.uid);
			_.uidToNodeMap[node.uid] = node;
			_.idToNodeMap[node.id] = node;
		},

		initApp : function() {
			if (appInitialized)
				return;
			appInitialized = true;

			_.displaySignupMessage();
			// alert('app initializing');

			console.log("initApp running.");
			$(window).on('orientationchange', _.orientationHandler);
			_.defineAllActions();

			_.deviceWidth = $(window).width();
			_.deviceHeight = $(window).height();

			/*
			 * This call checks the server to see if we have a session already,
			 * and gets back the login information from the session, and then
			 * renders page content, after that.
			 */
			user.refreshLogin();

			pageMgr.initializePageBuilders();

			/*
			 * Check for screen size in a timer. We don't want to monitor actual
			 * screen resize events because if a user is expanding a window we
			 * basically want to limit the CPU and chaos that would ensue if we
			 * tried to adjust things every time it changes. So we throttle back
			 * to only reorganizing the screen once per second. This timer is a
			 * throttle sort of.
			 */
			setInterval(function() {
				var width = $(window).width();

				if (width != _.deviceWidth) {
					// console.log("Screen width changed: " + width);

					_.deviceWidth = width;
					_.deviceHeight = $(window).height();

					_.screenSizeChange();
				}
			}, 1500);
		},

		displaySignupMessage : function() {
			var signupResponse = $("#signupCodeResponse").text();
			if (signupResponse === "ok") {
				alert("Signup complete. You may now login.");
			}
		},

		screenSizeChange : function() {
			if (_.currentNodeData) {
				$.each(_.currentNodeData.children, function(i, node) {
					if (node.imgId) {
						render.adjustImageSize(node);
					}
				});
			}
		},

		/* Don't need this method yet, and haven't tested to see if works */
		orientationHandler : function(event) {
			// if (event.orientation) {
			// if (event.orientation === 'portrait') {
			// } else if (event.orientation === 'landscape') {
			// }
			// }
		},

		loadAnonPageHome : function(ignoreUrl) {
			util.json("anonPageLoad", {
				"ignoreUrl" : ignoreUrl
			}, _.anonPageLoadResponse);
		}
	};

	// I decided no to use this technique to generate page content. I'm using
	// meta64.showPage
	// to ensure pages get created. Leaving this commented out in case it's
	// needed for something in the future.
	// $(document).on("pagecontainerbeforechange", function(event, data) {
	//
	// if (typeof data.toPage == "string") {
	// pageMgr.buildPage(data.toPage);
	// //}
	// }
	//
	// // else if (typeof toPage == "object") {
	// // }
	// });

	console.log("Module ready: meta64.js");
	return _;
}();

// # sourceURL=meta64.js
