console.log("running module: render.js");

var render = function() {

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
			"class" : "ui-btn ui-btn-inline ui-icon-star ui-btn-icon-left"
		}, "Create Content");

		return createSubNodeButton;
	}

	function _renderBinary(node) {
		/*
		 * If this is an image render the image directly onto the page as a
		 * visible image
		 */
		if (node.binaryIsImage) {
			return _.makeImageTag(node);
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
				"class" : "binary-link"
			}, anchor);
		}
	}

	var _ = {
		/*
		 * Important little method here. All GUI page/divs are created using
		 * this sort of specification here that they all must have a 'build'
		 * method that is called first time only, and then the 'init' method
		 * called before each time the component gets displayed with new
		 * information.
		 */
		buildPage : function(pg) {
			if (!pg.built) {
				pg.build();
				pg.built = true;
			}

			if (pg.init) {
				pg.init();
			}
		},

		/*
		 * node: JSON of NodeInfo.java
		 */
		renderNodeContent : function(node, showPath, showName, renderBinary) {
			var headerText = "";
			var ret = "";

			ret += _.getTopRightImageTag(node);

			var commentBy = props.getNodePropertyVal(jcrCnst.COMMENT_BY, node);
			if (showPath && meta64.editMode) {
				/*
				 * todo: come up with a solid plan for wether to show jcr:root
				 * to end users or not
				 */
				// var path = meta64.isAdminUser ? node.path :
				// node.path.replaceAll("/root", "");
				// var path = node.path;
				/* tail end of path is the name, so we can strip that off */
				// path = path.replace(node.name, "");
				headerText += "<div class='path-display'>Path: " + _.formatPath(node) + "</div>";

				headerText += "<div>";

				if (commentBy) {
					var clazz = (commentBy === meta64.userName) ? "created-by-me" : "created-by-other";
					headerText += "<span class='" + clazz + "'>Comment By: " + commentBy + "</span>";
				} //
				else if (node.createdBy) {
					var clazz = (node.createdBy === meta64.userName) ? "created-by-me" : "created-by-other";
					headerText += "<span class='" + clazz + "'>Created By: " + node.createdBy + "</span>";
				}

				headerText += "<span id='ownerDisplay" + node.uid + "'></span>";
				if (node.lastModified) {
					headerText += "  Modified: " + node.lastModified;
				}
				headerText += "</div>";
			}

			/*
			 * on root node name will be empty string so don't show that
			 * 
			 * commenting: I decided users will understand the path as a single
			 * long entity with less confusion than breaking out the name for
			 * them. They already unserstand internet URLs. This is the same
			 * concept. No need to baby them.
			 * 
			 * The !showPath condition here is because if we are showing the
			 * path then the end of that is always the name, so we don't need to
			 * show the path AND the name. One is a substring of the other.
			 */
			if (showName && !showPath && node.name) {
				headerText += "Name: " + node.name + " [uid=" + node.uid + "]";
			}

			if (headerText) {
				ret += _.makeTag("div", {
					"class" : "header-text"
				}, headerText);
			}

			if (meta64.showProperties) {
				// console.log("showProperties = " +
				// meta64.showProperties);
				var properties = props.renderProperties(node.properties);
				if (properties) {
					ret += /* "<br>" + */properties;
				}
			} else {
				var contentProp = props.getNodeProperty(jcrCnst.CONTENT, node);
				// console.log("contentProp: " + contentProp);
				if (contentProp) {

					var jcrContent = props.renderProperty(contentProp);

					if (jcrContent.length > 0) {
						ret += _.makeTag("div", {
							"class" : "jcr-content"
						}, _.wrapHtml(jcrContent));
					}
				}
			}

			if (renderBinary && node.hasBinary) {
				var binary = _renderBinary(node);

				/*
				 * We append the binary image or resource link either at the end
				 * of the text or at the location where the user has put
				 * {{insert-attachment}} if they are using that to make the
				 * image appear in a specific locatio in the content text.
				 */
				if (ret.contains(cnst.INSERT_ATTACHMENT)) {
					ret = ret.replaceAll(cnst.INSERT_ATTACHMENT, binary);
				} else {
					ret += binary;
				}
			}

			/*
			 * If this is a comment node, but not by the current user (because
			 * they cannot reply to themselves) then add a reply button, so they
			 * can reply to some other use.
			 */
			if (commentBy && commentBy != meta64.userName) {
				var replyButton = _.makeTag("a", //
				{
					"onClick" : "edit.replyToComment('" + node.uid + "');", //
					"class" : "ui-btn ui-btn-b ui-btn-inline ui-icon-plus ui-mini ui-btn-icon-comment"
				}, //
				"Reply");
				ret += replyButton;
			}
			/*
			 * Otherwise check if this is a publicly appendable node and show a
			 * button that does same as above but is labeled "Add Comment"
			 * instead of "Reply". Note the check against userName, makes sure
			 * the button doesn't show up on nodes we own, so we never are asked
			 * to "Add Comment" to our own content.
			 */
			else {
				var publicAppend = props.getNodePropertyVal(jcrCnst.PUBLIC_APPEND, node);
				if (publicAppend && commentBy != meta64.userName) {
					var addComment = _.makeTag("a", //
					{
						"onClick" : "edit.replyToComment('" + node.uid + "');", //
						"class" : "ui-btn ui-btn-b ui-btn-inline ui-icon-plus ui-mini ui-btn-icon-comment"
					}, //
					"Add Comment");
					ret += addComment;
				}
			}

			return ret;
		},

		/*
		 * This is the primary method for rendering each node (like a row) on
		 * the main HTML page that displays node content. This generates the
		 * HTML for a single row/node.
		 * 
		 * node is a NodeInfo.java JSON
		 */
		renderNodeAsListItem : function(node, index, count, rowCount) {

			var uid = node.uid;
			var selected = (node.id === meta64.newChildNodeId);
			var canMoveUp = index > 0 && rowCount > 1;
			var canMoveDown = index < count - 1;

			/*
			 * TODO: this checking of "rep:" is just a hack for now to stop from
			 * deleting things I won't want to allow to delete, but I will
			 * design this better later.
			 */
			var isRep = node.name.startsWith("rep:") || meta64.currentNodeData.node.path.contains("/rep:");
			var editingAllowed = meta64.isAdminUser || !isRep;

			/*
			 * if not selected by being the new child, then we try to select
			 * based on if this node was the last one clicked on for this page.
			 */
			// console.log("test: [" + parentIdToFocusIdMap[currentNodeId]
			// +"]==["+ node.id + "]")
			var focusNode = meta64.getHighlightedNode();
			if (!selected && focusNode && focusNode.uid === uid) {
				selected = true;
			}

			var buttonBarHtml = _.makeRowButtonBarHtml(uid, canMoveUp, canMoveDown, editingAllowed);

			var bkgStyle = _.getNodeBkgImageStyle(node);

			var cssId = uid + "_row";
			// console.log("Rendering Node Row[" + index + "] with id: " +cssId)
			return _.makeTag("div", //
			{
				"class" : "node-table-row" + (selected ? " active-row" : " inactive-row"),
				"onClick" : "nav.clickOnNodeRow(this, '" + uid + "');", //
				"id" : cssId,
				"style" : bkgStyle
			},// 
			buttonBarHtml + _.makeTag("div", //
			{
				"id" : uid + "_content"
			}, _.renderNodeContent(node, true, true, true)));
		},

		showNodeUrl : function() {
			var node = meta64.getHighlightedNode();
			if (!node) {
				alert("You must first click on a node.");
				return;
			}
			var url = window.location.origin + "?id=" + node.path;
			meta64.jqueryChangePage("#mainPage");

			var message = "URL using path: <br>" + url;
			var uuid = props.getNodePropertyVal("jcr:uuid", node);
			if (uuid) {
				message += "<p>URL for UUID: <br>" + window.location.origin + "?id=" + uuid;
			}

			messagePg.showMessage("URL of Node", message);
		},

		getTopRightImageTag : function(node) {
			var topRightImg = props.getNodePropertyVal('img.top.right', node);
			var topRightImgTag = "";
			if (topRightImg) {
				topRightImgTag = _.makeTag("img", {
					"src" : topRightImg,
					"class" : "top-right-image"
				}, "", false);
			}
			return topRightImgTag;
		},

		getNodeBkgImageStyle : function(node) {
			var bkgImg = props.getNodePropertyVal('img.node.bkg', node);
			var bkgImgStyle = "";
			if (bkgImg) {
				bkgImgStyle = "background-image: url(" + bkgImg + ");";
			}
			return bkgImgStyle;
		},

		/*
		 * TODO: This approach is going to be too heavy weight with large
		 * numbers of rows, so I will need to make the main menu dropdown
		 * operate on selected nodes when this happens (like rows > 100) rather
		 * than having buttons on all rows.
		 */
		makeRowButtonBarHtml : function(uid, canMoveUp, canMoveDown, editingAllowed) {

			/* TODO: make this a user option, each user can choose button sizes */
			var mini = "true";

			var openButton = selButton = createSubNodeButton = editNodeButton = //
			moveNodeUpButton = moveNodeDownButton = insertNodeButton = "";

			/* Construct Open Button */
			if (_.nodeHasChildren(uid)) {
				openButton = _.makeTag("a", //
				{
					"onClick" : "nav.openNode('" + uid + "');", //
					"class" : "ui-btn ui-btn-b ui-btn-inline ui-icon-plus ui-mini ui-btn-icon-left"
				}, //
				"Open");
			}

			/*
			 * If in edit mode we always at least create the potential (buttons)
			 * for a user to insert content, and if they don't have privileges
			 * the server side security will let them know. In the future we can
			 * add more intelligence to when to show these buttons or not.
			 */
			if (meta64.editMode) {
				// console.log("Editing allowed: " + nodeId);

				var selClass = meta64.selectedNodes[uid] ? "ui-btn-b" : "ui-btn-a";

				selButton = _.makeTag("a", //
				{
					"id" : uid + "_sel",//
					"onClick" : "nav.toggleNodeSel('" + uid + "');",
					// I tried ui-btn-icon-notext (and the button rendering is
					// bad) ???
					"class" : "ui-btn ui-btn-inline ui-icon-check ui-mini ui-btn-icon-left " + selClass
				}, "Sel");

				if (cnst.NEW_ON_TOOLBAR) {
					/* Construct Create Subnode Button */
					createSubNodeButton = _.makeTag("a", //
					{
						"onClick" : "edit.createSubNode('" + uid + "');",
						"class" : "ui-btn ui-btn-inline ui-icon-star ui-mini ui-btn-icon-left"
					}, "New");
				}

				if (cnst.INS_ON_TOOLBAR) {
					/* Construct Create Subnode Button */
					insertNodeButton = _.makeTag("a", //
					{
						"onClick" : "edit.insertNode('" + uid + "');",
						"class" : "ui-btn ui-btn-inline ui-icon-bars ui-mini ui-btn-icon-left"
					}, "Ins");
				}
			}

			if (meta64.editMode && editingAllowed) {

				/* Construct Create Subnode Button */
				editNodeButton = _.makeTag("a", //
				{
					"onClick" : "edit.runEditNode('" + uid + "');",
					"class" : "ui-btn ui-btn-inline ui-icon-edit ui-mini ui-btn-icon-left"
				}, "Edit");

				if (meta64.currentNode.childrenOrdered) {

					if (canMoveUp) {
						/* Construct Create Subnode Button */
						moveNodeUpButton = _.makeTag("a", //
						{
							"onClick" : "edit.moveNodeUp('" + uid + "');",
							"class" : "ui-btn ui-btn-inline ui-icon-arrow-u ui-mini ui-btn-icon-left"
						}, "Up");
					}

					if (canMoveDown) {
						/* Construct Create Subnode Button */
						moveNodeDownButton = _.makeTag("a", //
						{
							"onClick" : "edit.moveNodeDown('" + uid + "');",
							"class" : "ui-btn ui-btn-inline ui-icon-arrow-d ui-mini ui-btn-icon-left"
						}, "Dn");
					}
				}
			}

			var allButtons = selButton + openButton + insertNodeButton + createSubNodeButton + editNodeButton + moveNodeUpButton
					+ moveNodeDownButton;

			if (allButtons.length > 0) {
				return _.makeHorizontalFieldSet(allButtons, "compact-field-contain");
			} else {
				return "";
			}
		},

		makeHorizontalFieldSet : function(content, extraClasses) {
			/* Now build entire control bar */
			var buttonBar = _.makeTag("fieldset", //
			{
				"data-role" : "controlgroup", //
				"data-type" : "horizontal"
			}, content);

			return _.makeTag("div", {
				"class" : "ui-field-contain" + (extraClasses ? (" " + extraClasses) : "")
			}, buttonBar);
		},

		makeHorzControlGroup : function(content) {
			/* Now build entire control bar */
			return _.makeTag("div", //
			{
				"data-role" : "controlgroup", //
				"data-type" : "horizontal"
			}, content);
		},

		makeRadioButton : function(name, group, id, on) {
			return _.makeTag("input", //
			{
				"type" : "radio", //
				"name" : group,
				"id" : id,
				"checked" : on ? "checked" : "unchecked"
			}, "", true) + // + //

			_.makeTag("label", {
				"for" : id
			}, name);
		},

		/*
		 * Returns true if the nodeId (see makeNodeId()) NodeInfo object has
		 * 'hasChildren' true
		 */
		nodeHasChildren : function(uid) {
			var node = meta64.uidToNodeMap[uid];
			if (!node) {
				console.log("Unknown nodeId in nodeHasChildren: " + uid);
				return false;
			} else {
				return node.hasChildren;
			}
		},

		formatPath : function(node) {
			var path = node.path;
			/*
			 * TODO: This will fail now that jcr: is removed because it can
			 * match and corrupt any path that happens to start with root!
			 * BEWARE! FIX!
			 */
			var ret = meta64.isAdminUser ? path : path.replaceAll("/root", "");
			ret += " [" + node.primaryTypeName + "]";
			return ret;
		},

		wrapHtml : function(text) {
			return "<div data-ajax='false'>" + text + "</div>";
		},

		/*
		 * Each page can show buttons at the top of it (not main header buttons
		 * but additional buttons just for that page only, and this genates that
		 * content for that entire control bar.
		 */
		renderMainPageControls : function() {
			var html = '';

			if (srch.numSearchResults() > 0) {

				html += render.makeTag("a", //
				{
					"onClick" : "nav.showSearchPage();", //
					"class" : "ui-btn ui-btn-inline ui-icon-search ui-btn-icon-left"
				}, //
				"Back to Search Results");
			}

			var hasContent = html.length > 0;
			if (hasContent) {
				// $("#mainPageControls").html(html);
				util.setHtmlEnhanced($("#mainPageControls"), html);
			}

			util.setVisibility("#mainPageControls", hasContent)
		},

		renderPageFromData : function(data) {

			var newData = false;
			if (!data) {
				data = meta64.currentNodeData;
			} else {
				newData = true;
			}

			if (!data || !data.node) {
				util.setVisibility("#listView", false);
				$("#mainNodeContent").html("No default content is available.");
				return;
			} else {
				util.setVisibility("#listView", true);
			}

			_.renderMainPageControls();

			meta64.treeDirty = false;

			if (newData) {
				meta64.uidToNodeMap = {};
				meta64.idToNodeMap = {};

				/*
				 * I'm choosing to reset selected nodes when a new page loads,
				 * but this is not a requirement. I just don't have a "clear
				 * selections" feature which would be needed so user has a way
				 * to clear out.
				 */
				meta64.selectedNodes = {};

				meta64.initNode(data.node);
				meta64.setCurrentNodeData(data);
			}

			var propCount = meta64.currentNode.properties ? meta64.currentNode.properties.length : 0;
			// console.log("RENDER NODE: " + data.node.id + " propCount=" +
			// propCount);
			var output = '';

			var bkgStyle = _.getNodeBkgImageStyle(data.node);

			var mainNodeContent = _.renderNodeContent(data.node, true, true, true);
			// console.log("mainNodeContent: "+mainNodeContent);
			if (mainNodeContent.length > 0) {
				var uid = data.node.uid;
				var cssId = uid + "_row";

				var buttonBar = "";

				/* Add edit button if edit mode and this isn't the root */
				if (meta64.editMode && data.node.path != "/") {

					/* Construct Create Subnode Button */
					var editNodeButton = _.makeTag("a", //
					{
						"onClick" : "edit.runEditNode('" + uid + "');",
						"class" : "ui-btn ui-btn-inline ui-icon-edit ui-mini ui-btn-icon-left"
					}, "Edit");
					buttonBar = _.makeHorizontalFieldSet(editNodeButton, "compact-field-contain page-top-button-bar");
				}

				var content = _.makeTag("div", //
				{
					"class" : "node-table-row page-parent-node inactive-row",
					"onClick" : "nav.clickOnNodeRow(this, '" + uid + "');", //
					"id" : cssId,
					"style" : bkgStyle
				},// 
				buttonBar + mainNodeContent);

				$("#mainNodeContent").show();
				$("#mainNodeContent").html(content);
			} else {
				$("#mainNodeContent").hide();
			}
			$("#mainNodeContent").trigger("updatelayout");

			// console.log("update status bar.");
			view.updateStatusBar();

			// console.log("rendering page controls.");
			_.renderMainPageControls();

			if (data.children) {
				var childCount = data.children.length;
				// console.log("childCount: " + childCount);
				/*
				 * Number of rows that have actually made it onto the page to
				 * far. Note: some nodes get filtered out on the client side for
				 * various reasons.
				 */
				var rowCount = 0;

				$.each(data.children, function(i, node) {
					var row = _.generateRow(i, node, newData, childCount, rowCount);
					if (row.length != 0) {
						output += row;
						rowCount++;
					}
				});
			}

			if (output.length == 0 && !meta64.isAnonUser) {
				output = _getEmptyPagePrompt();
			}

			util.setHtmlEnhancedById("#listView", output);

			/*
			 * TODO: Instead of calling screenSizeChange here immediately, it
			 * would be better to set the image sizes exactly on the attributes
			 * of each image, as the HTML text is rendered before we even call
			 * setHtmlEnhancedById, so that images always are GUARANTEED to
			 * render correctly immediately.
			 */
			meta64.screenSizeChange();

			if (!meta64.getHighlightedNode()) {
				util.scrollToTop();
			}
		},

		generateRow : function(i, node, newData, childCount, rowCount) {

			if (meta64.isNodeBlackListed(node))
				return "";

			if (newData) {
				meta64.initNode(node);

				// console.log(" RENDER ROW[" + i + "]: node.id=" +
				// node.id);
			}

			rowCount++; // warning: this is the local variable/parameter
			var row = _.renderNodeAsListItem(node, i, childCount, rowCount);
			// console.log("row[" + rowCount + "]=" + row);
			return row;
		},

		getUrlForNodeAttachment : function(node) {
			return postTargetUrl + "bin/file-name?nodeId=" + encodeURIComponent(node.path) + "&ver=" + node.binVer;
		},

		/* see also: makeImageTag() */
		adjustImageSize : function(node) {

			var elm = $("#" + node.imgId);
			if (elm) {
				// var width = elm.attr("width");
				// var height = elm.attr("height");
				// console.log("width=" + width + " height=" + height);

				if (node.width && node.height) {

					if (node.width > meta64.deviceWidth - 80) {

						/* set the width we want to go for */
						// var width = meta64.deviceWidth - 80;
						/*
						 * and set the height to the value it needs to be at for
						 * same w/h ratio (no image stretching)
						 */
						// var height = width * node.height / node.width;
						elm.attr("width", "100%");
						elm.attr("height", "auto");
					}
					/* Image does fit on screen so render it at it's exact size */
					else {
						elm.attr("width", node.width);
						elm.attr("height", node.height);
					}
				}
			}
		},

		/* see also: adjustImageSize() */
		makeImageTag : function(node) {
			var src = _.getUrlForNodeAttachment(node);
			node.imgId = "imgUid_" + node.uid;

			if (node.width && node.height) {

				/*
				 * if image won't fit on screen we want to size it down to fit
				 * 
				 * Yes, it would have been simpler to just use something like
				 * width=100% for the image width but then the hight would not
				 * be set explicitly and that would mean that as images are
				 * loading into the page, the effective scroll position of each
				 * row will be increasing each time the URL request for a new
				 * image completes. What we want is to have it so that once we
				 * set the scroll position to scroll a particular row into view,
				 * it will stay the correct scroll location EVEN AS the images
				 * are streaming in asynchronously.
				 * 
				 */
				if (node.width > meta64.deviceWidth - 50) {

					/* set the width we want to go for */
					var width = meta64.deviceWidth - 50;

					/*
					 * and set the height to the value it needs to be at for
					 * same w/h ratio (no image stretching)
					 */
					var height = width * node.height / node.width;

					return _.makeTag("img", {
						"src" : src,
						"id" : node.imgId,
						"width" : width + "px",
						"height" : height + "px"
					}, null, false);
				}
				/* Image does fit on screen so render it at it's exact size */
				else {
					return _.makeTag("img", {
						"src" : src,
						"id" : node.imgId,
						"width" : node.width + "px",
						"height" : node.height + "px"
					}, null, false);
				}
			} else {
				return _.makeTag("img", {
					"src" : src,
					"id" : node.imgId
				}, null, false);
			}
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

		makeEditField : function(fieldName, fieldId) {
			return _.makeTag("label", {
				"for" : fieldId
			}, fieldName) + //
			_.makeTag("input", {
				"type" : "text",
				"name" : fieldId,
				"id" : fieldId
			}, "", true);
		},

		makePasswordField : function(fieldName, fieldId) {
			return _.makeTag("label", {
				"for" : fieldId
			}, fieldName) + //
			_.makeTag("input", {
				"type" : "password",
				"name" : fieldId,
				"id" : fieldId
			}, "", true);
		},

		makeButton : function(text, id, theme, classes) {
			var clazz = "ui-btn ui-btn-inline ui-btn-" + theme;
			if (classes) {
				clazz += " " + classes;
			}
			return render.makeTag("a", {
				"id" : id,
				"class" : clazz
			}, text);
		},

		makeBackButton : function(text, id, theme) {
			return render.makeTag("a", {
				"id" : id,
				"class" : "ui-btn ui-icon-carat-l ui-btn-inline ui-btn-" + theme,
				"data-rel" : "back"
			}, text);
		},

		allowPropertyToDisplay : function(propName) {
			return meta64.simpleModePropertyBlackList[propName] == null;
		},

		isReadOnlyProperty : function(propName) {
			return meta64.readOnlyPropertyList[propName];
		},

		isBinaryProperty : function(propName) {
			return meta64.binaryPropertyList[propName];
		},

		sanitizePropertyName : function(propName) {
			if (meta64.editModeOption === "simple") {
				return propName === jcrCnst.CONTENT ? "Content" : propName;
			} else {
				return propName;
			}
		}
	};

	console.log("Module ready: render.js");
	return _;
}();

// # sourceUrl=render.js
