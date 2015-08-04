console.log("running module: edit.js");

var edit = function() {

	/*
	 * node (NodeInfo.java) that is being created under when new node is created
	 */
	var _parentOfNewNode;
	var _sendNotificationPendingSave;

	var _saveNodeResponse = function(res) {
		util.checkSuccess("Save node", res);

		view.refreshTree(null, false);
		meta64.jqueryChangePage("#mainPage");
		view.scrollToSelectedNode();
	}

	var _renameNodeResponse = function(res) {
		util.checkSuccess("Rename node", res);

		view.refreshTree(null, false);
		meta64.jqueryChangePage("#mainPage");
		view.scrollToSelectedNode();
	}

	var _exportResponse = function(res) {
		if (util.checkSuccess("Export", res)) {
			alert("Export Successful.");
			meta64.jqueryChangePage("#mainPage");
			view.scrollToSelectedNode();
		}
	}

	var _importResponse = function(res) {
		if (util.checkSuccess("Import", res)) {
			alert("Import Successful.");
			view.refreshTree(null, false);
			meta64.jqueryChangePage("#mainPage");
			view.scrollToSelectedNode();
		}
	}

	var _insertBookResponse = function(res) {
		console.log("insertBookResponse running.");

		util.checkSuccess("Insert Book", res);
		view.refreshTree(null, false);
		meta64.jqueryChangePage("#mainPage");
		view.scrollToSelectedNode();
	}

	var _deleteNodesResponse = function(res) {
		if (util.checkSuccess("Delete node", res)) {
			meta64.clearSelectedNodes();
			view.refreshTree(null, false);
		}
	}

	var _moveNodesResponse = function(res) {
		util.checkSuccess("Move nodes", res);

		_.nodesToMove = null; // reset
		view.refreshTree(null, false);
	}

	var _setNodePositionResponse = function(res) {
		util.checkSuccess("Change node position", res);

		view.refreshTree(null, false);
		meta64.jqueryChangePage("#mainPage");
		view.scrollToSelectedNode();
	}

	var _insertNodeResponse = function(res) {
		util.checkSuccess("Insert node", res);

		/*
		 * TODO: verify this value gets used now that we aren't going
		 * IMMEDIATELY to the treeview after creates
		 */
		meta64.newChildNodeId = res.newNode.id;
		meta64.initNode(res.newNode);
		meta64.highlightNode(res.newNode, true);

		edit.runEditNode(res.newNode.uid);
	}

	var _makeNodeReferencableResponse = function(res) {
		if (util.checkSuccess("Make node referencable", res)) {
			alert("This node is now referencable, and can be accessed by unique ID");
		}

		// todo: need to refresh gui here to reflect change!
	}

	var _createSubNodeResponse = function(res) {
		util.checkSuccess("Create subnode", res);

		/*
		 * TODO: verify this value gets used now that we aren't going
		 * IMMEDIATELY to the treeview after creates
		 */
		meta64.newChildNodeId = res.newNode.id;
		console.log("new child identifier: " + meta64.newChildNodeId);

		meta64.initNode(res.newNode);
		edit.runEditNode(res.newNode.uid);
	}

	var _ = {
		/*
		 * Node ID array of nodes that are ready to be moved when user clicks
		 * 'Finish Moving'
		 */
		nodesToMove : null,

		/*
		 * indicates editor is displaying a node that is not yet saved on the
		 * server
		 */
		editingUnsavedNode : false,

		/* Node being edited */
		editNode : null,

		/*
		 * type=NodeInfo.java
		 * 
		 * When inserting a new node, this holds the node that was clicked on at
		 * the time the insert was requested, and is sent to server for ordinal
		 * position assignment of new node. Also if this var is null, it
		 * indicates we are creating in a 'create under parent' mode, versus
		 * non-null meaning 'insert inline' type of insert.
		 * 
		 */
		nodeInsertTarget : null,

		startEditingNewNode : function() {
			_.editingUnsavedNode = false;
			_.editNode = null;
			_.saveNewNode("");
		},

		/*
		 * called to display editor that will come up BEFORE any node is saved
		 * onto the server, so that the first time any save is performed we will
		 * have the correct node name, at least.
		 * 
		 * This version is no longer being used, and currently this means
		 * 'editingUnsavedNode' is not currently ever triggered. The new
		 * approach now that we have the ability to 'rename' nodes is to just
		 * create one with a random name an let user start editing right away
		 * and then rename the node IF a custom node name is needed.
		 * 
		 * What this means is if we call this function
		 * (startEditingNewNodeWithName) instead of 'startEditingNewNode()' that
		 * will cause the GUI to always prompt for the node name before creting
		 * the node. This was the original functionality and still works.
		 */
		startEditingNewNodeWithName : function() {
			_.editingUnsavedNode = true;
			_.editNode = null;
			meta64.changePage(editNodePg);
		},

		editMode : function() {
			meta64.editMode = meta64.editMode ? false : true;
			var elm = $("#editModeButton");
			elm.toggleClass("ui-icon-edit", meta64.editMode);
			elm.toggleClass("ui-icon-forbidden", !meta64.editMode);
			render.renderPageFromData();

			/*
			 * Since edit mode turns on lots of buttons, the location of the
			 * node we are viewing can change so much it goes completely
			 * offscreen out of view, so we scroll it back into view every time
			 */
			view.scrollToSelectedNode();
		},

		makeNodeReferencable : function() {
			util.json("makeNodeReferencable", {
				"nodeId" : _.editNode.id
			}, _makeNodeReferencableResponse);
		},

		cancelEdit : function() {

			if (meta64.treeDirty) {
				/*
				 * TODO: this results in a call to the server to refresh page
				 * which CAN be avoided if I write smarter client-side code, but
				 * for now, to get a product up and running soon, i'm just
				 * calling refresh here for a full blown call to server to
				 * refresh.
				 */
				// console.log("cancel edit, detected dirty, and will call
				// server.");
				view.refreshTree(null, false);

				/*
				 * if I had the logic in place to simply update client variables
				 * then I could call this simply (and avoid a call to server)
				 */
				// renderPageFromData(currentNodeData);
			}
			meta64.jqueryChangePage("#mainPage");
			view.scrollToSelectedNode();
		},

		/*
		 * for now just let server side choke on invalid things. It has enough
		 * security and validation to at least protect itself from any kind of
		 * damage.
		 */
		saveNode : function() {

			/*
			 * If editing an unsaved node it's time to run the insertNode, or
			 * createSubNode, which actually saves onto the server, and will
			 * initiate further editing like for properties, etc.
			 */
			if (_.editingUnsavedNode) {
				_.saveNewNode();
			}
			/*
			 * Else we are editing a saved node, which is already saved on
			 * server.
			 */
			else {
				_.saveExistingNode();
			}
		},

		saveNewNode : function(newNodeName) {
			if (!newNodeName) {
				newNodeName = util.getRequiredElement("#newNodeNameId").val();
			}

			/*
			 * If we didn't create the node we are inserting under, and neither
			 * did "admin", then we need to send notification email upon saving
			 * this new node.
			 */
			if (meta64.userName != _parentOfNewNode.createdBy && //
			_parentOfNewNode.createdBy != "admin") {
				_sendNotificationPendingSave = true;
			}

			meta64.treeDirty = true;
			if (_.nodeInsertTarget) {
				util.json("insertNode", {
					"parentId" : _parentOfNewNode.id,
					"targetName" : _.nodeInsertTarget.name,
					"newNodeName" : newNodeName
				}, _insertNodeResponse);
			} else {
				util.json("createSubNode", {
					"nodeId" : _parentOfNewNode.id,
					"newNodeName" : newNodeName
				}, _createSubNodeResponse);
			}
		},

		saveExistingNode : function() {
			var propertiesList = [];
			var counter = 0;
			var changeCount = 0;

			// iterate for all fields we can find
			while (true) {
				var fieldId = "editNodeTextContent" + counter;

				/* is this an existing gui edit field */
				if (meta64.fieldIdToPropMap.hasOwnProperty(fieldId)) {
					var prop = meta64.fieldIdToPropMap[fieldId];

					// alert('prop found: ' + prop.name);
					var propVal = $("#" + fieldId).val();

					if (propVal !== prop.value) {
						propertiesList.push({
							"name" : prop.name,
							"value" : propVal
						});

						changeCount++;
					}
				} else {
					break;
				}
				counter++;
			}

			if (changeCount > 0) {
				var postData = {
					nodeId : _.editNode.id,
					properties : propertiesList,
					sendNotification : _sendNotificationPendingSave
				};
				// alert(JSON.stringify(postData));
				util.json("saveNode", postData, _saveNodeResponse);
				_sendNotificationPendingSave = false;
			} else {
				alert("You didn't change any information!");
			}
		},

		moveNodeUp : function(uid) {
			var node = meta64.uidToNodeMap[uid];
			if (node) {
				var ordinal = meta64.getOrdinalOfNode(node);
				console.log("ordinal=" + ordinal);
				if (ordinal == -1 && ordinal <= 0)
					return;
				var nodeAbove = meta64.currentNodeData.children[ordinal - 1];

				util.json("setNodePosition", {
					"parentNodeId" : meta64.currentNodeId,
					"nodeId" : node.name,
					"siblingId" : nodeAbove.name
				}, _setNodePositionResponse);
			} else {
				console.log("idToNodeMap does not contain " + uid);
			}
		},

		moveNodeDown : function(uid) {
			var node = meta64.uidToNodeMap[uid];
			if (node) {
				var ordinal = meta64.getOrdinalOfNode(node);
				console.log("ordinal=" + ordinal);
				if (ordinal == -1 && ordinal >= meta64.currentNodeData.children.length - 1)
					return;
				var nodeBelow = meta64.currentNodeData.children[ordinal + 1];

				util.json("setNodePosition", {
					"parentNodeId" : meta64.currentNodeData.node.id,
					"nodeId" : nodeBelow.name,
					"siblingId" : node.name
				}, _setNodePositionResponse);
			} else {
				console.log("idToNodeMap does not contain " + uid);
			}
		},

		openExportPg : function() {
			meta64.changePage(exportPg);
		},

		openRenameNodePg : function() {
			meta64.changePage2(renameNodePg); //"#renameNodePg");
		},

		renameNode : function() {
			var newName = $("#newNodeNameEditField").val();

			if (util.emptyString(newName)) {
				alert("Please enter a new node name.");
				return;
			}

			var highlightNode = meta64.getHighlightedNode();
			if (!highlightNode) {
				alert("Select a node to rename.");
				return;
			}

			util.json("renameNode", {
				"nodeId" : highlightNode.id,
				"newName" : newName
			}, _renameNodeResponse);
		},

		exportNodes : function() {
			var highlightNode = meta64.getHighlightedNode();
			var targetFileName = util.getRequiredElement("#exportTargetNodeName").val();

			if (util.emptyString(targetFileName)) {
				alert("Please enter a name for the export file.");
				return;
			}

			if (highlightNode) {
				util.json("exportToXml", {
					"nodeId" : highlightNode.id,
					"targetFileName" : targetFileName
				}, _exportResponse);
			}
		},

		openImportPg : function() {
			meta64.changePage(importPg);
		},

		importNodes : function() {
			var highlightNode = meta64.getHighlightedNode();
			var sourceFileName = util.getRequiredElement("#importTargetNodeName").val();

			if (util.emptyString(sourceFileName)) {
				alert("Please enter a name for the import file.");
				return;
			}

			if (highlightNode) {
				util.json("import", {
					"nodeId" : highlightNode.id,
					"sourceFileName" : sourceFileName
				}, _importResponse);
			}
		},

		runEditNode : function(uid) {
			var node = meta64.uidToNodeMap[uid];
			if (!node) {
				_.editNode = null;
				alert("Unknown nodeId in editNodeClick: " + uid);
				return;
			}
			_.editingUnsavedNode = false;
			_.editNode = node;
			// _.populateEditNodePg();
			meta64.changePage(editNodePg);
		},

		/*
		 * Generates all the HTML edit fields and puts them into the DOM model
		 * of the property editor dialog box.
		 * 
		 */
		populateEditNodePg : function() {

			/* display the node path at the top of the edit page */
			view.initEditPathDisplayById("#editNodePathDisplay");

			var fields = '';
			var counter = 0;

			/* clear this map to get rid of old properties */
			meta64.fieldIdToPropMap = {};

			/* TODO: this block of code nests too deep. clean this up! */

			/* editNode will be null if this is a new node being created */
			if (_.editNode) {
				// Iterate PropertyInfo.java objects
				$.each(_.editNode.properties, function(index, prop) {
					if (!render.allowPropertyToDisplay(prop.name)) {
						console.log("Hiding property: " + prop.name);
						return;
					}

					var fieldId = "editNodeTextContent" + counter;

					meta64.fieldIdToPropMap[fieldId] = prop;
					var isMulti = prop.values && prop.values.length > 0;

					var isReadOnlyProp = render.isReadOnlyProperty(prop.name);
					var isBinaryProp = render.isBinaryProperty(prop.name);

					/*
					 * this is the way (for now) that we make sure this node
					 * won't be attempted to be saved. If it has RdOnly_ prefix
					 * it won't be found by the saving logic.
					 */
					if (isReadOnlyProp || isBinaryProp) {
						fieldId = "RdOnly_" + fieldId;
					}

					var buttonBar = "";

					if (!isReadOnlyProp && !isBinaryProp) {
						var clearButton = render.makeTag("a", //
						{
							"onClick" : "props.clearProperty('" + fieldId + "');", //
							"class" : "ui-btn ui-btn-inline ui-icon-back ui-btn-icon-left"
						}, //
						"Clear");

						var addMultiButton = "";
						var deleteButton = "";

						if (prop.name !== jcrCnst.CONTENT) {
							/*
							 * For now we just go with the design where the
							 * actual content property cannot be deleted. User
							 * can leave content blank but not delete it.
							 */
							deleteButton = render.makeTag("a", //
							{
								"onClick" : "props.deleteProperty('" + prop.name + "');", //
								// "onClick" : function() {
								// props.deleteProperty(prop.name);
								// }, //
								"class" : "ui-btn ui-btn-inline ui-icon-delete ui-btn-icon-left"
							}, //
							"Del");

							/*
							 * I don't think it really makes sense to allow a
							 * jcr:content property to be multivalued. I may be
							 * wrong but this is my current assumption
							 */
							addMultiButton = render.makeTag("a", //
							{
								"onClick" : "props.addSubProperty('" + fieldId + "');", //
								"class" : "ui-btn ui-btn-inline ui-icon-star ui-btn-icon-left"
							}, //
							"Add Multi");
						}

						var allButtons = addMultiButton + clearButton + deleteButton;
						if (allButtons.length > 0) {
							buttonBar = render.makeHorizontalFieldSet(allButtons);
						} else {
							buttonBar = "";
						}
					}

					var field = buttonBar;

					if (isMulti) {
						console.log("Property multi-type: name=" + prop.name + " count=" + prop.values.length);
						field += props.makeMultiPropEditor(fieldId, prop, isReadOnlyProp, isBinaryProp);
					} else {
						console.log("Property single-type: " + prop.name);
						field += render.makeTag("label", {
							"for" : fieldId
						}, render.sanitizePropertyName(prop.name));

						var propVal = isBinaryProp ? "[binary]" : prop.value;

						if (isReadOnlyProp || isBinaryProp) {
							field += render.makeTag("textarea", {
								"id" : fieldId,
								"readonly" : "readonly",
								"disabled" : "disabled"
							}, propVal ? propVal : '');
						} else {
							field += render.makeTag("textarea", {
								"id" : fieldId
							}, propVal ? propVal : '');
						}
					}

					fields += render.makeTag("div", {
						"class" : "ui-field-contain"
					}, field);

					counter++;
				});

			} else {
				var field = render.makeTag("label", {
					"for" : "newNodeNameId"
				}, "New Node Name") //
						+ render.makeTag("textarea", {
							"id" : "newNodeNameId"
						}, '');

				fields += render.makeTag("div", {
					"class" : "ui-field-contain"
				}, field);
			}
			util.setHtmlEnhanced($("#propertyEditFieldContainer"), fields);

			var instr = _.editingUnsavedNode ? //
			"You may leave this field blank and a unique ID will be assigned. You only need to provide a name if you want this node to have a more meaningful URL."
					: //
					"";

			$("#editNodeInstructions").html(instr);

			/*
			 * Allow adding of new properties as long as this is a saved node we
			 * are editing, because we don't want to start managing new
			 * properties on the client side. We need a genuine node already
			 * saved on the server before we allow any property editing to
			 * happen.
			 */
			util.setVisibility("#addPropertyButton", !_.editingUnsavedNode);

			util.setVisibility("#makeNodeReferencableButton", !_.editingUnsavedNode);
		},

		insertNode : function(uid) {

			_parentOfNewNode = meta64.currentNode;
			if (!_parentOfNewNode) {
				console.log("Unknown parent");
				return;
			}

			/*
			 * We get the node selected for the insert position by using the uid
			 * if one was passed in or using the currently highlighted node if
			 * no uid was passed.
			 */
			var node = null;
			if (!uid) {
				node = meta64.getHighlightedNode();
			} else {
				node = meta64.uidToNodeMap[uid];
			}

			if (node) {
				_.nodeInsertTarget = node;
				_.startEditingNewNode();
			}
		},

		createSubNodeUnderHighlight : function() {

			_parentOfNewNode = meta64.getHighlightedNode();
			if (!_parentOfNewNode) {
				alert("Tap a node to insert under.");
				return;
			}

			/*
			 * this indicates we are NOT inserting inline. An inline insert
			 * would always have a target.
			 */
			_.nodeInsertTarget = null;
			_.startEditingNewNode();
		},

		createSubNode : function(uid) {
			/*
			 * If no uid provided we deafult to creating a node under the
			 * currently viewed node (parent of current page)
			 */
			if (!uid) {
				_parentOfNewNode = meta64.currentNode;
			} else {
				_parentOfNewNode = meta64.uidToNodeMap[uid];
				if (!_parentOfNewNode) {
					console.log("Unknown nodeId in createSubNode: " + uid);
					return;
				}
			}

			/*
			 * this indicates we are NOT inserting inline. An inline insert
			 * would always have a target.
			 */
			_.nodeInsertTarget = null;
			_.startEditingNewNode();
		},

		/*
		 * Delete the single node identified by 'uid' parameter if uid parameter
		 * is passed, and if uid parameter is not passed then use the node
		 * selections for multiple selections on the page.
		 */
		deleteSelNodes : function() {

			var selNodesArray = meta64.getSelectedNodeIdsArray();
			if (!selNodesArray || selNodesArray.length == 0) {
				alert('You have not selected any nodes. Select nodes to delete first.');
				return;
			}

			confirmPg.areYouSure("Confirm Delete", "Delete " + selNodesArray.length + " node(s) ?", "Yes, delete.", function() {

				util.json("deleteNodes", {
					"nodeIds" : selNodesArray
				}, _deleteNodesResponse);
			});
		},

		moveSelNodes : function() {

			var selNodesArray = meta64.getSelectedNodeIdsArray();
			if (!selNodesArray || selNodesArray.length == 0) {
				alert('You have not selected any nodes. Select nodes to delete first.');
				return;
			}

			confirmPg.areYouSure("Confirm Move", "Move " + selNodesArray.length + " node(s) to a new location ?", "Yes, move.", function() {
				_.nodesToMove = selNodesArray;
				meta64.selectedNodes = {}; // clear selections. No longer need
				// or want any selections.
				alert("Ok, ready to move nodes. To finish moving, go select the target location, then click 'Finish Moving'");
				meta64.refreshAllGuiEnablement();
			});
		},

		finishMovingSelNodes : function() {
			confirmPg.areYouSure("Confirm Move", "Move " + _.nodesToMove.length + " node(s) to selected location ?", "Yes, move.",
					function() {

						var highlightNode = meta64.getHighlightedNode();

						/*
						 * For now, we will just cram the nodes onto the end of
						 * the children of the currently selected page. Later on
						 * we can get more specific about allowing precise
						 * destination location for moved nodes.
						 */
						util.json("moveNodes", {
							"targetNodeId" : highlightNode.id,
							"targetChildId" : highlightNode != null ? highlightNode.id : null,
							"nodeIds" : _.nodesToMove
						}, _moveNodesResponse);
					});
		},

		insertBookWarAndPeace : function() {

			confirmPg.areYouSure("Confirm", "Insert book War and Peace?", "Yes, insert book.", function() {

				/* inserting under whatever node user has focused */
				var node = meta64.getHighlightedNode();

				if (!node) {
					alert("No node is selected.");
				} else {
					util.json("insertBook", {
						"nodeId" : node.id,
						"bookName" : "War and Peace"
					}, _insertBookResponse);
				}
			});
		}
	};

	console.log("Module ready: edit.js");
	return _;
}();

// # sourceURL=edit.js
