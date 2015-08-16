console.log("running module: messagePg.js");

var messagePg = function() {

	var _title, _message, _callback;

	var _ = {
		domId : "messagePg",

		showMessage : function(title, message, callback) {
			_title = title;
			_message = message;
			_callback = callback;
			meta64.openDialog(messagePg);
		},

		build : function() {

			var fields = "<h3 id='messagePgTitle'></h3><p id='messagePgMessage'></p>";
			fields += render.makeBackButton("Ok", "messagePgOkButton", "b");
			var content = render.makeTag("div", {
				"data-role" : "content"
			}, fields);

			util.setHtmlEnhanced($("#messagePg"), content);
			
			$("#messagePgOkButton").on("click", function() {
				_callback();
			});
		},

		init : function() {
			$("#messagePgTitle").text(_title);
			$("#messagePgMessage").html(_message);
		}
	};

	console.log("Module ready: messagePg.js");
	return _;
}();

// # sourceURL=messagePg.js
