console.log("running module: confirmPg.js");

var confirmPg = function() {

	var _title, _message, _buttonText, _callback;

	var _ = {
		domId : "confirmPg",

		areYouSure : function(title, message, buttonText, callback) {
			_title = title;
			_message = message;
			_buttonText = buttonText;
			_callback = callback;
			meta64.changePage(confirmPg);
		},

		build : function() {

			var fields = "<h3 id='confirmPgTitle'></h3><p id='confirmPgMessage'></p>";
			fields += render.makeBackButton("Yes", "confirmPgYesButton", "b");
			fields += render.makeBackButton("No", "confirmPgNoButton", "a");
			var content = render.makeTag("div", {
				"data-role" : "content"
			}, fields);

			util.setHtmlEnhanced($("#confirmPg"), content);
		},

		init : function() {
			$("#confirmPgTitle").text(_title);
			$("#confirmPgMessage").text(_message);
			$("#confirmPgYesButton").off("click");
			$("#confirmPgYesButton").text(_buttonText).on("click", function() {
				_callback();
				$(this).off("click");
			});
		}
	};

	console.log("Module ready: confirmPg.js");
	return _;
}();

// # sourceURL=confirmPg.js
