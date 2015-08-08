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

		simpleModePropertyBlackList : {},

		readOnlyPropertyList : {},

		binaryPropertyList : {},

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

		jqueryChangePage : function(pageName) {
			$.mobile.pageContainer.pagecontainer("change", pageName);
		},

		changePage : function(pg) {
			render.buildPage(pg);
			$.mobile.pageContainer.pagecontainer("change", "#" + pg.domId);
		},

		popup : function() {
			render.buildPage(popupMenuPg);
			$("#" + popupMenuPg.domId).popup("open");
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

		updateNodeInfoResponse : function(res, node) {
			var ownerBuf = '';
			// console.log("****** updateNodeInfoResponse: " +
			// JSON.stringify(res));
			var mine = false;

			if (res.owners) {
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
			}

			if (ownerBuf.length > 0) {
				node.owner = ownerBuf;
				var elm = $("#ownerDisplay" + node.uid);
				elm.html(" (Manager: " + ownerBuf + ")");
				if (mine) {
					util.changeOrAddClass(elm, "created-by-other", "created-by-me");
				} else {
					util.changeOrAddClass(elm, "created-by-me", "created-by-other");
				}
			}
		},

		updateNodeInfo : function(node) {
			var prms = util.json("getNodePrivileges", {
				"nodeId" : node.id,
				"includeAcl" : false,
				"includeOwners" : true
			});

			prms.done(function(res) {
				_.updateNodeInfoResponse(res, node);
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
		addClickListeners : function() {
			$("#openLoginPgButton").on("click", user.openLoginPg);
			$("#navHomeButton").on("click", nav.navHome);
			$("#navUpLevelButton").on("click", nav.navUpLevel);
			// $("#propsToggleButton").on("click", props.propsToggle);
			$("#deletePropertyButton").on("click", props.deleteProperty);
			$("#editModeButton").on("click", edit.editMode);
		},

		openDonatePg : function() {
			meta64.jqueryChangePage("#donatePg");
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

			util.setEnablement($("#navHomeButton"), _.currentNode && !nav.displayingHome());
			util.setEnablement($("#navUpLevelButton"), _.currentNode && nav.parentVisibleToUser());

			var propsToggle = _.currentNode && !_.isAnonUser;
			/*
			 * this leaves a hole in the toolbar if you hide it. Need to change
			 * that
			 */
			util.setEnablement($("#propsToggleButton"), propsToggle);

			util.setEnablement($("#deletePropertyButton"), !_.isAnonUser);

			var editMode = _.currentNode && !_.isAnonUser;
			// console.log(">>>>>>>>>>>>>>> currentNode=" + _.currentNode + "
			// anonUser=" + _.anonUser);
			/*
			 * this leaves a hole in the toolbar if you hide it. Need to change
			 * that
			 */
			util.setEnablement($("#editModeButton"), editMode);
			util.setEnablement($("#insNodeButton"), !_.isAnonUser && highlightNode != null);
			util.setEnablement($("#createNodeButton"), !_.isAnonUser && highlightNode != null);

			util.setVisibility("#menuButton", !_.isAnonUser);
			util.setVisibility("#openSignupPgButton", _.isAnonUser);
			util.setVisibility("#mainMenuSearchButton", !_.isAnonUser && highlightNode != null);
			util.setVisibility("#mainMenuTimelineButton", !_.isAnonUser && highlightNode != null);

			util.setVisibility("#mainNavBar", !_.isAnonUser);
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

		initConstants : function() {
			util.addAll(_.simpleModePropertyBlackList, [ jcrCnst.PRIMARY_TYPE, jcrCnst.POLICY ]);

			util.addAll(_.readOnlyPropertyList, [ jcrCnst.UUID, jcrCnst.MIXIN_TYPES, jcrCnst.CREATED, jcrCnst.CREATED_BY,
					jcrCnst.LAST_MODIFIED, jcrCnst.LAST_MODIFIED_BY, jcrCnst.IMG_WIDTH, jcrCnst.IMG_HEIGHT, jcrCnst.BIN_VER,
					jcrCnst.BIN_DATA, jcrCnst.BIN_MIME ]);

			util.addAll(_.binaryPropertyList, [ jcrCnst.BIN_DATA ]);
		},

		initApp : function() {
			if (appInitialized)
				return;
			console.log("initApp running.");
			appInitialized = true;

			_.initConstants();
			_.displaySignupMessage();

			$(window).on("orientationchange", _.orientationHandler);
			
			$(window).bind("beforeunload", function() {
				return "Leave Meta64 ?";
			});
			
			$(window).on("unload", function() {
				user.logout();
			});

			_.addClickListeners();

			_.deviceWidth = $(window).width();
			_.deviceHeight = $(window).height();

			/*
			 * This call checks the server to see if we have a session already,
			 * and gets back the login information from the session, and then
			 * renders page content, after that.
			 */
			user.refreshLogin();

			/*
			 * Check for screen size in a timer. We don't want to monitor actual
			 * screen resize events because if a user is expanding a window we
			 * basically want to limit the CPU and chaos that would ensue if we
			 * tried to adjust things every time it changes. So we throttle back
			 * to only reorganizing the screen once per second. This timer is a
			 * throttle sort of. Yes I know how to listen for events. No I'm not
			 * doing it wrong here. This timer is correct in this case and
			 * behaves superior to events.
			 */
			setInterval(function() {
				var width = $(window).width();

				if (width != _.deviceWidth) {
					console.log("Screen width changed: " + width);

					_.deviceWidth = width;
					_.deviceHeight = $(window).height();

					_.screenSizeChange();
				}
			}, 1500);

			_.refreshAllGuiEnablement();
		},

		displaySignupMessage : function() {
			var signupResponse = $("#signupCodeResponse").text();
			if (signupResponse === "ok") {
				alert("Signup complete. You may now login.");
			}
		},

		screenSizeChange : function() {
			if (_.currentNodeData) {

				if (meta64.currentNode.imgId) {
					render.adjustImageSize(meta64.currentNode);
				}

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
