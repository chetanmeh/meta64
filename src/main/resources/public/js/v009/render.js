console.log("running module: render.js");

var render = function() {

	var _markdown;

	/*
	 * ================= PRIVATE =================
	 */
	/*
	 * node: JSON of NodeInfo.java
	 */
	function _renderNodeContent(node, showIdentifier, showPath, showName, renderBinary) {
		var ret = '';

		if (showPath) {
			var path = meta64.js.isAdminUser ? node.path : node.path.replaceAll("/jcr:root", "");
			/* tail end of path is the name, so we can strip that off */
			// path = path.replace(node.name, "");
			ret += "Path: " + _.formatPath(path) + "<br>";
		}

		if (showIdentifier) {
			if (node.id === node.path && showPath) {
				// ret += "ID: *<br>";
			} else {
				/*
				 * if ID contains a slash then it's more path than ID, so I won't show it as an ID. This is kind of 
				 * confusing how JCR will put in the ID as a path component, which is valid but then again it's not
				 * an ID either.
				 */
				//if (!node.id.contains("/")) {
					ret += "ID: " + node.id + "<br>";
				//}
			}
		}

		/*
		 * on root node name will be empty string so don't show that
		 * 
		 * commenting: I decided users will understand the path as a single long
		 * entity with less confusion than breaking out the name for them. They
		 * already unserstand internet URLs. This is the same concept. No need
		 * to baby them.
		 * 
		 * The !showPath condition here is because if we are showing the path
		 * then the end of that is always the name, so we don't need to show the
		 * path AND the name. One is a substring of the other.
		 */
		if (showName && !showPath && node.name) {
			ret += "Name: " + node.name + " [uid=" + node.uid + "]";
		}

		if (meta64.js.showProperties) {
			// console.log("showProperties = " + meta64.js.showProperties);
			var properties = props.renderProperties(node.properties);
			if (properties) {
				ret += /* "<br>" + */properties;
			}
		} else {
			var contentProp = props.getNodeProperty("jcr:content", node);
			if (contentProp) {
				var jcrContent = props.renderProperty(contentProp);

				ret += _.makeTag("div", {
					"class" : "jcr-content"
				}, _.markdown(jcrContent));
			}
		}
		if (renderBinary && node.hasBinary) {
			ret += _renderBinary(node);
		}
		return ret;
	}

	/*
	 * This is the content displayed when the user signs in, and we see that
	 * they have no content being displayed. We want to give them some
	 * insructions and the ability to add content.
	 */
	function _getEmptyPagePrompt() {
		/* Construct Create Subnode Button */
		var createSubNodeButton = _.makeTag("a", //
		{
			"onClick" : "edit.createSubNode();",
			"data-role" : "button",
			"data-icon" : "star"
		}, "Create Content");

		return createSubNodeButton;
	}

	function _renderBinary(node) {
		/*
		 * If this is an image render the image directly onto the page as a
		 * visible image
		 */
		if (node.binaryIsImage) {
			return _.makeImageTagForNode(node);
		}
		/*
		 * If not an image we render a link to the attachment, so that it can be
		 * downloaded.
		 */
		else {
			var anchor = _.makeTag("a", {
				"href" : _.getUrlForNodeAttachment(node)
			}, "[Download Attachment]");

			return _.makeTag("div", {
				"class" : "binary-link",
			}, anchor);
		}
	}

	/*
	 * ================= PUBLIC =================
	 */
	var _ = {
		/*
		 * node is a NodeInfo.java JSON
		 */
		renderNodeAsListItem : function(node, index, count, rowCount) {

			var uid = node.uid;
			var selected = (node.id === meta64.js.newChildNodeId);
			var canMoveUp = index > 0 && rowCount > 1;
			var canMoveDown = index < count - 1;

			/*
			 * this checking of "rep:" is just a hack for now to stop from
			 * deleting things I won't want to allow to delete, but I will
			 * design this better later.
			 */
			var isRep = node.name.startsWith("rep:") || meta64.js.currentNodeData.node.path.contains("/rep:");
			var editingAllowed = meta64.isAdminUser || !isRep;

			/*
			 * if not selected by being the new child, then we try to select
			 * based on if this node was the last one clicked on for this page.
			 */
			// console.log("test: [" + parentIdToFocusIdMap[currentNodeId]
			// +"]==["+ node.id + "]")
			var focusNode = meta64.js.parentUidToFocusNodeMap[meta64.js.currentNodeUid];
			if (!selected && focusNode && focusNode.uid === uid) {
				selected = true;
			}

			var cssId = uid + "_row";
			// console.log("Rendering Node Row[" + index + "] with id: " +cssId)
			return _.makeTag("div", //
			{
				"class" : "node-table-row" + (selected ? " active-row" : " inactive-row"),
				"onClick" : "nav.clickOnNodeRow(this, '" + uid + "');", //
				"id" : cssId
			},// 
			_.makeButtonBarHtml(uid, canMoveUp, canMoveDown, editingAllowed) + _.makeTag("div", //
			{
				"id" : uid + "_content"
			}, _renderNodeContent(node, true, true, true, true)));
		},

		makeButtonBarHtml : function(uid, canMoveUp, canMoveDown, editingAllowed) {

			var openButton = selButton = createSubNodeButton = deleteNodeButton = editNodeButton = //
			moveNodeUpButton = moveNodeDownButton = insertNodeButton = editNodeSharingButton = uploadButton = '';

			/* Construct Open Button */
			if (_.nodeHasChildren(uid)) {
				openButton = _.makeTag("a", //
				{
					"onClick" : "nav.openNode('" + uid + "');", //
					"data-role" : "button",
					"data-icon" : "plus",
					"data-theme" : "b"
				}, //
				"Open");
			}

			/* Construct SelectButton */
			var checkboxHtml = _.makeTag("input", //
			{
				"type" : "checkbox", //
				"id" : uid + "_sel",//
				"name" : uid + "_check", //
				"onClick" : "nav.toggleNodeSel('" + uid + "')",
				"data-icon" : "bullets"
			}, "", false);
			selButton = _.makeTag("label", null, checkboxHtml + "Sel");

			/*
			 * If in edit mode we always at least create the potential (buttons)
			 * for a user to insert content, and if they don't have privileges
			 * the server side security will let them know. In the future we can
			 * add more intelligence to when to show these buttons or not.
			 */
			if (meta64.js.editMode) {
				// console.log("Editing allowed: " + nodeId);

				/* Construct Create Subnode Button */
				createSubNodeButton = _.makeTag("a", //
				{
					"onClick" : "edit.createSubNode('" + uid + "');",
					"data-role" : "button",
					"data-icon" : "star"
				}, "New");

				/* Construct Create Subnode Button */
				insertNodeButton = _.makeTag("a", //
				{
					"onClick" : "edit.insertNode('" + uid + "');",
					"data-role" : "button",
					"data-icon" : "bars"
				}, "Ins");
			}

			if (meta64.js.editMode && editingAllowed) {
				/* Construct Create Subnode Button */
				deleteNodeButton = _.makeTag("a", //
				{
					"onClick" : "edit.deleteNode('" + uid + "');",
					"data-role" : "button",
					"data-icon" : "delete"
				}, "Del");

				/* Construct Create Subnode Button */
				editNodeButton = _.makeTag("a", //
				{
					"onClick" : "edit.editNode('" + uid + "');",
					"data-role" : "button",
					"data-icon" : "edit"
				}, "Edit");

				/* Construct Create Subnode Button */
				uploadButton = _.makeTag("a", //
				{
					"onClick" : "attachment.openUploadDialog('" + uid + "');",
					"data-role" : "button",
					"data-icon" : "action"
				}, "Upload");

				if (meta64.js.currentNode.childrenOrdered) {

					if (canMoveUp) {
						/* Construct Create Subnode Button */
						moveNodeUpButton = _.makeTag("a", //
						{
							"onClick" : "edit.moveNodeUp('" + uid + "');",
							"data-role" : "button",
							"data-icon" : "arrow-u"
						}, "Up");
					}

					if (canMoveDown) {
						/* Construct Create Subnode Button */
						moveNodeDownButton = _.makeTag("a", //
						{
							"onClick" : "edit.moveNodeDown('" + uid + "');",
							"data-role" : "button",
							"data-icon" : "arrow-d"
						}, "Dn");
					}
				}

				/* Construct Create Subnode Button */
				editNodeSharingButton = _.makeTag("a", //
				{
					"onClick" : "share.editNodeSharing('" + uid + "');",
					"data-role" : "button",
					"data-icon" : "eye"
				}, "Share");
			}

			return _.makeHorizontalFieldSet(selButton + openButton + insertNodeButton + createSubNodeButton + editNodeButton + uploadButton
					+ deleteNodeButton + moveNodeUpButton + moveNodeDownButton + editNodeSharingButton);
		},

		makeHorizontalFieldSet : function(content) {
			/* Now build entire control bar */
			var buttonBar = _.makeTag("fieldset", //
			{
				"data-role" : "controlgroup", //
				"data-type" : "horizontal"
			}, content);

			return _.makeTag("div", {
				"class" : "ui-field-contain"
			}, buttonBar);
		},

		/*
		 * Returns true if the nodeId (see makeNodeId()) NodeInfo object has
		 * 'hasChildren' true
		 */
		nodeHasChildren : function(uid) {
			var node = meta64.js.uidToNodeMap[uid];
			if (!node) {
				console.log("Unknown nodeId in nodeHasChildren: " + uid);
				return false;
			} else {
				return node.hasChildren;
			}
		},

		formatPath : function(path) {
			return meta64.js.isAdminUser ? path : path.replaceAll("/jcr:root", "");
		},

		markdown : function(text) {

			/*
			 * I will lazy load this function just in case this helps overall
			 * app startup time
			 */
			if (!_markdown) {
				_markdown = new Markdown.getSanitizingConverter().makeHtml;
			}

			return _markdown(text);
		},

		renderPageFromData : function(data) {
			var newData = false;
			if (!data) {
				data = meta64.js.currentNodeData;
			} else {
				newData = true;
			}

			meta64.js.treeDirty = false;

			// console.log("renderPageFromData.");
			if (newData) {
				meta64.js.uidToNodeMap = {};
				meta64.initNode(data.node);
				meta64.setCurrentNodeData(data);
			}

			var propCount = meta64.js.currentNode.properties ? meta64.js.currentNode.properties.length : 0;
			// console.log("RENDER NODE: " + data.node.id + propertyCount=" +
			// propCount);
			var output = '';

			$("#mainNodeContent").html(_renderNodeContent(data.node, true, true, false, false));
			view.updateStatusBar();

			var childCount = data.children.length;
			
			/* Number of rows that have actually made it onto the page to far. Note: some nodes get filtered 
			 * out on the client side for various reasons.
			 */
			var rowCount = 0;

			$.each(data.children, function(i, node) {
				if (meta64.isNodeBlackListed(node)) return;
				
				if (newData) {
					meta64.initNode(node);

					// console.log(" RENDER ROW[" + i + "]: node.id=" +
					// node.id);

					/*
					 * if no row is selected for this parent, select the first
					 * row
					 */
					if (!meta64.js.parentUidToFocusNodeMap[meta64.js.currentNodeUid]) {
						meta64.js.parentUidToFocusNodeMap[meta64.js.currentNodeUid] = node;

						if (!node.uid) {
							alert("oops, node.uid is null");
						}
						// console.log("Setting default row selection to this
						// top
						// row");
					} else {
						// console.log(" SEL ROW
						// KNOWN:"+parentIdToFocusIdMap[meta64.currentNodeData.node.id]);
					}
				}

				rowCount++;
				output += _.renderNodeAsListItem(node, i, childCount, rowCount);
			});

			if (output.length == 0) {
				output = _getEmptyPagePrompt();
			}

			util.setHtmlEnhanced($("#listView"), output);

			view.scrollToSelectedNode();
		},

		getUrlForNodeAttachment : function(node) {
			return postTargetUrl + "bin?nodeId=" + encodeURIComponent(node.path + "/nt:bin") + "&ver=" + node.binVer;
		},

		makeImageTagForNode : function(node) {
			return _.makeTag("img", {
				"src" : _.getUrlForNodeAttachment(node)
			}, null, false);
		},

		/*
		 * creates HTML tag with all attributes/values specified in attributes
		 * object, and closes the tag also if content is non-null
		 */
		makeTag : function(tag, attributes, content, closeTag) {

			/* default parameter values */
			if (typeof (closeTag) === 'undefined')
				closeTag = true;

			/* HTML tag itself */
			var ret = "<" + tag;

			if (attributes) {
				ret += " ";
				$.each(attributes, function(k, v) {
					/*
					 * we intelligently wrap strings that contain single quotes
					 * in double quotes and vice versa
					 */
					if (v.contains("'")) {
						ret += k + "=\"" + v + "\" ";
					} else {
						ret += k + "='" + v + "' ";
					}
				});
			}

			if (closeTag) {
				ret += ">" + content + "</" + tag + ">";
			} else {
				ret += "/>";
			}

			return ret;
		},

		allowPropertyToDisplay : function(propName) {
			return meta64.js.simpleModePropertyBlackList[propName] == null;
		},

		isReadOnlyProperty : function(propName) {
			return meta64.js.readOnlyPropertyList[propName];
		},

		isBinaryProperty : function(propName) {
			return meta64.js.binaryPropertyList[propName];
		},

		sanitizePropertyName : function(propName) {
			if (meta64.js.editModeOption === "simple") {
				return propName === "jcr:content" ? "Content" : propName;
			} else {
				return propName;
			}
		}
	};

	console.log("Module ready: render.js");
	return _;
}();