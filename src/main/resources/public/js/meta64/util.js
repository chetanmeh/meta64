console.log("running module: util.js");

var util = function() {

	var logAjax = false;

	Date.prototype.stdTimezoneOffset = function() {
		var jan = new Date(this.getFullYear(), 0, 1);
		var jul = new Date(this.getFullYear(), 6, 1);
		return Math.max(jan.getTimezoneOffset(), jul.getTimezoneOffset());
	}

	Date.prototype.dst = function() {
		return this.getTimezoneOffset() < this.stdTimezoneOffset();
	}

	if (typeof String.prototype.startsWith != 'function') {
		String.prototype.startsWith = function(str) {
			return this.indexOf(str) === 0;
		};
	}

	if (typeof String.prototype.contains != 'function') {
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

	/*
	 * We use this variable to determine if we are waiting for an ajax call, but
	 * the server also enforces that each session is only allowed one concurrent
	 * call and simultaneous calls would just "queue up".
	 */
	var _ajaxCounter = 0;

	var _ = {

		daylightSavingsTime : (new Date().dst()) ? true : false,

		/**
		 * We use the convention that all calls to server are POSTs with a
		 * 'postName' (like an RPC method name)
		 * <p>
		 * Note: 'callback' can be null, if you want to use the returned
		 * 'promise' rather than passing in a function.
		 * 
		 */
		json : function(postName, postData, callback) {

			if (logAjax) {
				console.log("JSON-POST: " + JSON.stringify(postData));
			}

			_ajaxCounter++;
			var prms = $.ajax({
				url : postTargetUrl + postName,
				contentType : "application/json",
				type : "post",
				dataType : "json",
				cache : false,
				data : JSON.stringify(postData)
			});

			/**
			 * Notes
			 * <p>
			 * If using then function: promise.then(successFunction,
			 * failFunction);
			 * <p>
			 * I think the way these parameters get passed into done/fail
			 * functions, is because there are resolve/reject methods getting
			 * called with the parameters. Basically the parameters passed to
			 * 'resolve' get distributed to all the waiting methods just like as
			 * if they were subscribing in a pub/sub model. So the 'promose'
			 * pattern is sort of a pub/sub model in a way
			 * <p>
			 * The reason to return a 'promise.promise()' method is so no other
			 * code can call resolve/reject but can only react to a
			 * done/fail/complete.
			 * <p>
			 * deferred.when(promise1, promise2) creates a new promise that
			 * becomes 'resolved' only when all promises are resolved. It's a
			 * big "and condition" of resolvement, and if any of the promises
			 * passed to it end up failing, it fails this "ANDed" one also.
			 */
			prms.done(function(jqXHR) {
				if (logAjax) {
					console.log("JSON-RESULT: " + postName + "\nJSON-RESULT-DATA: " + JSON.stringify(jqXHR));
				}

				if (typeof callback == "function") {
					callback(jqXHR);
				}
			});

			prms.fail(function(xhr, status) {
				alert("Server request failed."); // xhr.responseText);
				// //JSON.parse(xhr.responseText));
			});

			prms.complete(function() {
				_ajaxCounter--;
			});

			return prms;
		},

		ajaxReady : function(requestName) {
			if (_ajaxCounter > 0) {
				console.log("Ignoring requests: " + requestName + ". Ajax currently in progress.");
				return false;
			}
			return true;
		},

		isAjaxWaiting : function() {
			return _ajaxCounter > 0;
		},

		/* set focus to element by id (id must start with #) */
		delayedFocus : function(id) {
			setTimeout(function() {
				$(id).focus();
			}, 500);
		},

		/*
		 * We could have put this logic inside the json method itself, but I can
		 * forsee cases where we don't want a message to appear when the json
		 * response returns success==false, so we will have to call checkSuccess
		 * inside every response method instead, if we want that response to
		 * print a message to the user when fail happens.
		 */
		checkSuccess : function(opFriendlyName, res) {
			if (!res.success) {
				alert(opFriendlyName + " failed: " + res.message);
			}
			return res.success;
		},

		/* adds all array objects to obj as a set */
		addAll : function(obj, a) {
			for (var i = 0; i < a.length; i++) {
				if (!a[i]) {
					console.error("null element in addAll at idx=" + i);
				} else {
					obj[a[i]] = true;
				}
			}
		},

		nullOrUndef : function(obj) {
			return obj === null || obj === undefined;
		},

		/*
		 * We have to be able to map any identifier to a uid, that will be
		 * repeatable, so we have to use a local 'hashset-type' implementation
		 */
		getUidForId : function(map, id) {
			/* look for uid in map */
			var uid = map[id];

			/* if not found, get next number, and add to map */
			if (!uid) {
				uid = meta64.nextUid++;
				map[id] = uid;
			}
			return uid;
		},

		getPreviousPage : function() {
			// get the ID of the previous page
			var prevPage = '#' + $.mobile.activePage.prev('div[data-role="page"]')[0].id;
			alert('prevPage=' + prevPage);
			return prevPage;
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
			if (!e || e.length == 0) {
				console.log("domElm Error. Required element id not found: " + id);
			}
			return e;
		},

		/*
		 * Gets the element and displays an error message if it's not found
		 */
		getRequiredElement : function(id) {
			var e = $(id);
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

		setEnablement : function(elm, enable, visibility) {
			/*
			 * optional parameter, undefined means it wasn't passed which means
			 * 'true' in this case
			 */
			if (typeof (visibility) === 'undefined' || visibility) {
				_.setVisibility(elm, true)
			} else {
				_.setVisibility(elm, false);
			}

			if (enable)
				elm.removeClass('ui-state-disabled');
			if (!enable)
				elm.addClass('ui-state-disabled');
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

		bindEnterKey : function(id, func) {
			_.bindKey(id, func, 13);
		},

		bindKey : function(id, func, keyCode) {
			$(id).keypress(function(e) {
				if (e.which == keyCode) { // 13==enter key code
					func();
					return false;
				}
			});
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

		// part of some troubleshooting i'll eventually delete.
		// setHtmlEnhancedById2 : function(elmId, content) {
		// console.log("setting content: "+content+" on id "+elmId);
		// //var elm = _.getRequiredElement(elmId);
		// var elm = document.getElementById(elmId);
		// if (elm) {
		// console.log("setting innerHTML is successful");
		// elm.innerHtml = content;
		// //elm.html(content);
		// //elm.enhanceWithin();
		// }
		// else {
		// console.log("setting failed.");
		// }
		// },

		setHtmlEnhancedById : function(elmId, content) {
			// console.log("setting content: "+content+" on id "+elmId);
			var elm = _.getRequiredElement(elmId);
			if (elm) {
				$(elm).html(content);
				$(elm).enhanceWithin();
				// console.log("setting is successful");
			} else {
				console.log("setting failed.");
			}
		},

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

		/*
		 * iterates over an object creating a string containing it's keys and
		 * values
		 */
		printProperties : function(obj) {
			if (!obj) {
				console.error("printProperties recieved null.");
				return;
			}
			var val = '';
			$.each(obj, function(k, v) {
				val += k + "\n";
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
				// console.log("Showing element: " + elmId);
				$(elmId).show();
			} else {
				// console.log("hiding element: " + elmId);
				$(elmId).hide();
			}
			$(elmId).trigger("updatelayout");
		}
	};

	console.log("Module ready: util.js");
	return _;
}();

//# sourceURL=util.js

