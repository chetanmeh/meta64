console.log("running module: props.js");

var props = function() {

	var _savePropertyResponse = function(res) {
		util.checkSuccess("Save properties", res);

		edit.editNode.properties.push(res.propertySaved)
		meta64.changePage("#editNodePg");
		meta64.treeDirty = true;
	}

	var _deletePropertyResponse = function(res, propertyToDelete) {
		// alert("info: "+JSON.stringify(info));
		if (util.checkSuccess("Delete property", res)) {

			/*
			 * remove deleted property from client side storage, so we can
			 * re-render screen without making another call to server
			 */
			props.deletePropertyFromLocalData(propertyToDelete);

			/* now just re-render screen from local variables */
			meta64.changePage("#editNodePg");
			meta64.treeDirty = true;
		}
	}

	var _ = {
		/*
		 * Toggles display of properties in the gui.
		 */
		propsToggle : function() {
			meta64.showProperties = meta64.showProperties ? false : true;
			// setDataIconUsingId("#editModeButton", editMode ? "edit" :
			// "forbidden");
			
			/*
			 * TODO: this button icon needs to change now that the properties is on the main manu 
			 * instead of navbar button. Currently not functional.
			 */
			var elm = $("#propsToggleButton");
			elm.toggleClass("ui-icon-grid", meta64.showProperties);
			elm.toggleClass("ui-icon-forbidden", !meta64.showProperties);
			render.renderPageFromData();
			view.scrollToSelectedNode();
			meta64.changePage("#mainPage");
		},

		/*
		 * Deletes the property of the specified name on the node being edited,
		 * but first gets confirmation from user
		 */
		deleteProperty : function(propName) {

			confirmPg.areYouSure("Confirm Delete", "Delete the Property", "Yes, delete.", function() {
				_.deletePropertyImmediate(propName);
			});
		},

		deletePropertyImmediate : function(propName) {

			var prms = util.json("deleteProperty", {
				"nodeId" : edit.editNode.id,
				"propName" : propName
			});

			prms.done(function(res) {
				_deletePropertyResponse(res, propName);
			});
		},

		addProperty : function() {
			meta64.changePage("#editPropertyPg");
		},

		populatePropertyEdit : function() {
			var field = '';

			/* Property Name Field */
			{
				var fieldPropNameId = "addPropertyNameTextContent";

				field += render.makeTag("label", {
					"for" : fieldPropNameId
				}, "Name");

				field += render.makeTag("textarea", {
					"name" : fieldPropNameId,
					"id" : fieldPropNameId,
					"placeholder" : "Enter property name"
				}, "");
			}

			/* Property Value Field */
			{
				var fieldPropValueId = "addPropertyValueTextContent";

				field += render.makeTag("label", {
					"for" : fieldPropValueId
				}, "Value");

				field += render.makeTag("textarea", {
					"name" : fieldPropValueId,
					"id" : fieldPropValueId,
					"placeholder" : "Enter property text"
				}, "");
			}

			/* display the node path at the top of the edit page */
			view.initEditPathDisplayById("#editPropertyPathDisplay");

			util.setHtmlEnhanced($("#addPropertyFieldContainer"), field);
		},

		saveProperty : function() {
			var propertyNameData = $("#addPropertyNameTextContent").val();
			var propertyValueData = $("#addPropertyValueTextContent").val();

			var postData = {
				nodeId : edit.editNode.id,
				propertyName : propertyNameData,
				propertyValue : propertyValueData
			};
			// alert(JSON.stringify(postData));
			util.json("saveProperty", postData, _savePropertyResponse);
		},

		addSubProperty : function(fieldId) {
			var prop = meta64.fieldIdToPropMap[fieldId];

			var isMulti = util.isObject(prop.values);

			/* convert to multi-type if we need to */
			if (!isMulti) {
				prop.values = [];
				prop.values.push(prop.value);
				prop.value = null;
			}

			/*
			 * now add new empty property and populate it onto the screen
			 * 
			 * TODO: for performance I can do something simpler than
			 * 'populateEditNodeDialog' here, but for now just re-rendering the
			 * entire edit page is what I'm doing for simplicity.
			 */
			prop.values.push('');
			edit.populateEditNodePg();
		},

		clearProperty : function(fieldId) {
			// console.log("clearing: " + fieldId);

			var elm = $("#" + fieldId);
			/* checking length is the way to see if the property exists */
			if (elm.length != 0) {
				elm.val('');
			}

			/* scan for all multi-value property fields and clear them */
			var counter = 0;
			while (counter < 1000) {
				elm = $("#" + fieldId + "_subProp" + counter);
				if (elm.length == 0)
					break;
				elm.val('');
				counter++;
			}
		},

		deletePropertyFromLocalData : function(propertyName) {
			for (var i = 0; i < edit.editNode.properties.length; i++) {
				if (propertyName === edit.editNode.properties[i].name) {
					// console.log("Delete id at index: " + i);
					// splice is how you delete array elements in js.
					edit.editNode.properties.splice(i, 1);
					break;
				}
			}
		},

		makeMultiPropEditor : function(fieldId, prop, isReadOnlyProp, isBinaryProp) {
			var fields = '';

			var propList = prop.values;
			if (!propList || propList.length == 0) {
				propList = [];
				propList.push("");
			}

			for (var i = 0; i < propList.length; i++) {
				console.log("prop multi-val[" + i + "]=" + propList[i]);
				var id = fieldId + "_subProp" + i;

				fields += render.makeTag("label", {
					"for" : id
				}, (i == 0 ? prop.name : "*") + "." + i);

				var propVal = isBinaryProp ? "[binary]" : propList[i];

				if (isBinaryProp || isReadOnlyProp) {
					fields += render.makeTag("textarea", {
						"id" : id,
						"readonly" : "readonly",
						"disabled" : "disabled"
					}, propVal ? propVal : '');
				} else {
					fields += render.makeTag("textarea", {
						"id" : id
					}, propVal ? propVal : '');
				}
			}
			return fields;
		},

		/*
		 * Orders properties in some consisten manor appropriate to display in
		 * gui. Currenetly all we are doing is moving any 'jcr:content' property
		 * to the beginning of the list
		 * 
		 * properties will be null or a list of PropertyInfo objects
		 */
		setPreferredPropertyOrder : function(properties) {
			if (!properties)
				return;

			var newList = [];
			$.each(properties, function(i, property) {
				if (property.name === jcrCnst.CONTENT) {
					/*
					 * unshift is how javascript adds an element to the head of
					 * an array shifting to the right any existing elements
					 */
					newList.unshift(property);
				} else {
					newList.push(property);
				}
			});
			return newList;
		},

		/*
		 * TODO: optimize string concats for performance.
		 * 
		 * properties will be null or a list of PropertyInfo objects
		 */
		renderProperties : function(properties) {
			if (properties) {
				var ret = "<table data-role='table' class='prop-table ui-responsive ui-shadow table-stripe property-text'>";
				var propCount = 0;

				/*
				 * We don't need or want a table header, but JQuery displays an
				 * error in the JS console if it can't find the <thead> element.
				 * So we provide empty tags here, just to make JQuery happy.
				 */
				ret += "<thead><tr><th></th><th></th></tr></thead>";

				ret += "<tbody>";
				$.each(properties, function(i, property) {
					if (render.allowPropertyToDisplay(property.name)) {
						var isBinaryProp = render.isBinaryProperty(property.name);

						propCount++;
						ret += "<tr class='prop-table-row'>";

						ret += "<td class='prop-table-name-col'>" + render.sanitizePropertyName(property.name) + "</td>";

						if (isBinaryProp) {
							ret += "<td class='prop-table-val-col'>[binary]</td>";
						} else if (!property.values) {
							ret += "<td class='prop-table-val-col'>" + render.markdown(property.value) + "</td>";
						} else {
							ret += "<td class='prop-table-val-col'>" + props.renderPropertyValues(property.values) + "</td>";
						}
						ret += "</tr>";
					} else {
						console.log("Hiding property: " + property.name);
					}
				});

				if (propCount == 0) {
					return "";
				}

				ret += "</tbody></table>";
				return ret;
			} else {
				return undefined;
			}
		},

		/*
		 * does brute force search on node (NodeInfo.java) object properties
		 * list, and returns the first property (PropertyInfo.java) with name
		 * matching propertyName, else null.
		 */
		getNodeProperty : function(propertyName, node) {
			if (!node || !node.properties)
				return null;

			for (var i = 0; i < node.properties.length; i++) {
				var prop = node.properties[i];
				if (prop.name === propertyName) {
					return prop;
				}
			}
			return null;
		},
		
		getNodePropertyVal : function(propertyName, node) {
			var prop = _.getNodeProperty(propertyName, node);
			return prop ? prop.value : null;
		},

		/*
		 * Returns string representation of property value, even if multiple
		 * properties
		 */
		renderProperty : function(property) {
			if (!property.values) {
				if (!property.value || property.value.length == 0) {
					return "";
				}
				return render.markdown(property.value);
			} else {
				return _.renderPropertyValues(property.values);
			}
		},

		renderPropertyValues : function(values) {
			var ret = "<div>";
			$.each(values, function(i, value) {
				ret += render.markdown(value) + cnst.BR;
			});
			ret += "</div>";
			return ret;
		}
	};

	console.log("Module ready: props.js");
	return _;
}();

// # sourceURL=props.js
