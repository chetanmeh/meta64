console.log("running module: loginPg.js");

var loginPg = function() {

	var _ = {
		build : function() {

			var header = render.makeTag("div", //
			{
				"data-role" : "header" //
			}, //
			"<h2>" + BRANDING_TITLE + " - Login</h2>");

			var formControls = render.makeEditField("User", "userName") + //
			render.makePasswordField("Password", "password");

			var loginButton = render.makeButton("Login", "loginButton", "b");
			var backButton = render.makeBackButton("Close", "cancelLoginButton", "a");
			var buttonBar = render.makeButtonBar(loginButton + backButton);

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
		},

		init : function() {
			// var loginButtonElm = $("#loginButton");
			// var logoutButtonElm = $("#logoutButton");
			//			
			// loginButtonElm.on("click", _.login);
			// logoutButtonElm.on("click", _.logout);
			//			
			var loginEnable = meta64.isAnonUser;

			console.log("loginEnable: " + loginEnable);
			// util.setEnablement(loginButtonElm, loginEnable);
			// util.setVisibility(loginButtonElm, loginEnable);
			//			
			// var logoutEnable = !meta64.isAnonUser;
			// console.log("loginEnable: "+logoutEnable);
			// util.setEnablement(logoutButtonElm, logoutEnable);
			// util.setVisibility(logoutButtonElm, logoutEnable);

			$("#loginButton").text(loginEnable ? "Login" : "Logout");
		}
	};

	console.log("Module ready: loginPg.js");
	return _;
}();

// # sourceURL=loginPg.js
