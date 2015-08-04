console.log("running module: loginPg.js");

var loginPg = function() {

	var _ = {
			domId : "loginPg",
		build : function() {

			var header = render.makeTag("div", //
			{
				"data-role" : "header"//,
				//"data-position" : "fixed",
				//"data-tap-toggle" : "false"
			}, //
			"<h2>" + BRANDING_TITLE + " - Login</h2>");

			var formControls = render.makeEditField("User", "userName") + //
			render.makePasswordField("Password", "password");

			var loginButton = render.makeButton("Login", "loginButton", "b", "ui-btn-icon-left ui-icon-check");
			var backButton = render.makeBackButton("Close", "cancelLoginButton", "a");
			var buttonBar = render.makeHorzControlGroup(loginButton + backButton);

			var form = render.makeTag("div", //
			{
				"class" : "ui-field-contain" //
			}, //
			formControls + buttonBar);

			var internalMainContent = "";
			var mainContent = render.makeTag("div", //
			{
				"role" : "main", //
				"class" : "ui-content"
			}, //
			internalMainContent + form);

			var content = header + mainContent;

			util.setHtmlEnhanced($("#loginPg"), content);

			$("#loginButton").on("click", user.login);
			util.bindEnterKey("#userName", user.login);
			util.bindEnterKey("#password", user.login);
		},

		init : function() {
			util.delayedFocus("#userName");
		}
	};

	console.log("Module ready: loginPg.js");
	return _;
}();

// # sourceURL=loginPg.js
