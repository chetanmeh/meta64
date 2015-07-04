console.log("running module: util.js");

var util = function() {

	/*
	 * ================= PRIVATE =================
	 */

	/*
	 * ================= PUBLIC =================
	 */
	if (typeof String.prototype.startsWith != 'function') {
		// see below for better implementation!
		String.prototype.startsWith = function(str) {
			return this.indexOf(str) === 0;
		};
	}

	if (typeof String.prototype.contains != 'function') {
		// see below for better implementation!
		String.prototype.contains = function(str) {
			return this.indexOf(str) != -1;
		};
	}

	function escapeRegExp(string) {
		return string.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
	}

	if (typeof String.prototype.replaceAll != 'function') {
		String.prototype.replaceAll = function(find, replace) {
			return this.replace(new RegExp(escapeRegExp(find), 'g'), replace);
		}
	}

	var assertNotNull = function(varName) {
		if (typeof eval(varName) === 'undefined') {
			alert("Variable not found: " + varName)
		}
	}

	var _ = {

		/*
		 * We use the convention that all calls to server are POSTs with a
		 * 'postName' (like an RPC method name), and the target url is formatted
		 * as [postName]Request
		 */
		json : function(postName, postData, callback, info) {
			if (typeof callback !== "function") {
				console.log("callback not valid function for postName " + postName);
			}

			console.log("JSON-POST: " + JSON.stringify(postData));
			$.ajax({
				url : postTargetUrl + postName,
				contentType : "application/json",
				type : "post",
				dataType : "json",
				cache : false,
				data : JSON.stringify(postData),
				success : function(jqXHR, textStatus) {
					console.log("JSON-RESULT: " + postName + " -> " + textStatus);
					console.log("JSON-RESULT-DATA: " + JSON.stringify(jqXHR));
					if (textStatus === "success") {
						callback(jqXHR, info);
					} else {
						console.log("JSON: " + postName + " -> " + textStatus);
					}
				},
				error : function(xhr, status, error) {
					alert("Server request failed."); // xhr.responseText);
														// //JSON.parse(xhr.responseText));
				}
			});
		},

		getPreviousPage : function() {
			// get the ID of the previous page
			var prevPage = '#' + $.mobile.activePage.prev('div[data-role="page"]')[0].id;
			alert('prevPage=' + prevPage);
			return prevPage;
		},

		areYouSure : function(title, message, buttonText, callback) {
			$("#sure .sure-1").text(title);
			$("#sure .sure-2").text(message);
			$("#sure .sure-do").text(buttonText).on("click.sure", function() {
				callback();
				$(this).off("click.sure");
			});
			$.mobile.changePage("#sure");
		},

		/*
		 * Gets the RAW DOM element and displays an error message if it's not
		 * found. Do not prefix with "#"
		 */
		domElm : function(id) {
			if (id.contains("#")) {
				console.log("do not use # in domElm");
				return null;
			}
			var e = document.getElementById(id);
			if (e.length == 0) {
				console.log("domElm Error. Required element id not found: " + id);
			}
			return e;
		},

		/*
		 * Gets the element and displays an error message if it's not found
		 */
		getRequiredElement : function(id) {
			// var e = document.getElementById(id);
			e = $(id);
			if (e == null) {
				console.log("getRequiredElement. Required element id not found: " + id);
			}
			return e;
		},

		isObject : function(obj) {
			return obj && obj.length != 0;
		},

		currentTimeMillis : function() {
			return new Date().getMilliseconds();
		},

		emptyString : function(val) {
			return !val || val.length == 0;
		},

		isActionEnabled : function(actionName) {
			var action = meta64.actionNameToObjMap[actionName];
			if (!action) {
				alert("Unrecognized actionName: " + actionName);
				return;
			}
			return action["enable"];
		},

		setEnablementByName : function(actionName, enablement, visibility) {
			/* first find action object, and update enablement */
			var action = meta64.actionNameToObjMap[actionName];
			if (!action) {
				alert("Unrecognized actionName: " + actionName);
				return;
			}
			action["enable"] = enablement;

			/* now update the DOM to reflect the enablement */
			var id = "#" + actionName + "Button";
			var elm = $(id);
			if (elm) {
				// console.log("setting enablement of " + id + " to " +
				// enablement);
				_.setEnablement(elm, enablement);
				
				/*
				 * optional parameter, undefined means it wasn't passed which
				 * means 'true' in this case
				 */
				if (typeof (visibility) === 'undefined' || visibility) {
					_.setVisibility(elm, true)
				}
				else {
					_.setVisibility(elm, false);
				}
			}
		},

		setEnablement : function(elm, enable) {
			if (enable)
				elm.removeClass('ui-disabled');
			if (!enable)
				elm.addClass('ui-disabled');
		},

		hookSliderChanges : function() {
			for (var i = 0; i < arguments.length; i++) {
				var funcName = arguments[i] + "SliderChange";
				var func = window[funcName];
				if (typeof func !== "function") {
					alert("Function not found: " + funcName);
				}
				var id = "#" + arguments[i] + "Slider";

				if (!hookSlider(id, func)) {
					alert("Failed to hook slider: " + arguments[i]);
					return;
				}
			}
		},

		hookSlider : function(id, func) {
			_.getRequiredElement(id).change(func);
			// function() {
			// var slider_value = $("#slider-1").val();
			// // do something..
			// });
			return true;
		},

		/*
		 * hooks click to element id making it call function 'func' when clicked
		 */
		hookClick : function(id, func) {
			// console.log("hookClick for domID: "+id);

			// Saw this also online:
			// $('a').bind('click', function () {
			// $('input').val(randomString[Math.floor(Math.random() * 3)]);
			// });

			_.getRequiredElement(id).click(func);
			return true;
		},

		anyEmpty : function() {
			for (var i = 0; i < arguments.length; i++) {
				var val = arguments[i];
				if (!val || val.length == 0)
					return true;
			}
			return false;
		},

		/*
		 * Removed oldClass from element and replaces with newClass, and if
		 * oldClass is not present it simply adds newClass. If old class
		 * existed, in the list of classes, then the new class will now be at
		 * that position. If old class didn't exist, then new Class is added at
		 * end of class list.
		 */
		changeOrAddClass : function(elm, oldClass, newClass) {
			var elm = $(elm);
			elm.toggleClass(oldClass, false);
			elm.toggleClass(newClass, true);
		},

		/*
		 * I stupidly wrote this implementation before I knew about
		 * 'toggleClass'. oops. My implementation works but isn't necessary
		 * AFAIK
		 */
		changeOrAddClass__mine : function(elm, oldClass, newClass) {

			if (!elm.className || elm.className.length == 0) {
				console.log("no existing className is available on element, so assigning to " + newClass);
				elm.className = newClass;
				return;
			}

			var classNames = elm.className.split(" ");
			var newNames = '';
			var newClassDone = false;

			for (var i = 0; i < classNames.length; i++) {
				var name = classNames[i];

				/*
				 * if we found the newClass in the list of classes, ignore, and
				 * don't add again
				 */
				if (name === newClass) {
					if (newClassDone)
						continue;

					newClassDone = true;
				} else {
					if (name === oldClass) {
						if (newClassDone)
							continue;
						name = newClass;
						newClassDone = true;
					}
				}

				if (newNames.length > 0) {
					newNames += " ";
				}
				newNames += name;
			}

			/*
			 * if we didn't replace the old class, or find the class existing,
			 * then append it to end
			 */
			if (!newClassDone) {
				if (newNames.length > 0) {
					newNames += " ";
				}
				newNames += newClass;
			}

			elm.className = newNames;
			// console.log("final class: " + elm.className);
		},

		/*
		 * displays message (msg) of object is not of specified type
		 */
		verifyType : function(obj, type, msg) {
			if (typeof obj !== type) {
				alert(msg);
				return false;
			}
			return true;
		},

		// function setDataIconUsingId(id, dataIcon) {
		// var elm = $(id);
		// elm.attr('data-icon', dataIcon);
		// elm.ehanceWithin();
		// }

		setHtmlEnhanced : function(elm, content) {
			elm.html(content);
			elm.enhanceWithin();
		},

		getPropertyCount : function(obj) {
			var count = 0;
			var prop;

			for (prop in obj) {
				if (obj.hasOwnProperty(prop)) {
					count++;
				}
			}
			return count;
		},

		/*
		 * iterates over an object creating a string containing it's keys and
		 * values
		 */
		printObject : function(obj) {
			var val = '';
			$.each(obj, function(k, v) {
				val += k + " , " + v + "\n";
			});
			return val;
		},

		scrollToTop : function() {
			// setTimeout(function() {
			window.scrollTo(0, 0);
			// }, 1);
		},

		/* iterates over an object creating a string containing it's keys */
		printKeys : function(obj) {
			var val = '';
			$.each(obj, function(k, v) {
				if (val.length > 0) {
					val += ',';
				}
				val += k;
			});
			return val;
		},

		/*
		 * Makes eleId visible based on vis flag
		 * 
		 * elmId: element id with no # prefix
		 */
		setVisibility : function(elmId, vis) {

			if (vis) {
				console.log("Showing element: " + elmId);
				$(elmId).show();
			} else {
				console.log("hiding element: " + elmId);
				$(elmId).hide();
			}
		}
	};

	console.log("Module ready: util.js");
	return _;
}();