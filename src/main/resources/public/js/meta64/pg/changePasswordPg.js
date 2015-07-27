console.log("running module: changePasswordPg.js");

var changePasswordPg = function() {

	var _ = {
		build : function() {

			var header = render.makeTag("div", //
			{
				"data-role" : "header"//,
				//"data-position" : "fixed",
				//"data-tap-toggle" : "false"
			}, //
			"<h2>" + BRANDING_TITLE + " - Login</h2>");

			var formControls = render.makePasswordField("Password", "changePassword1") + //
			render.makePasswordField("Repeat Password", "changePassword2");

			var changePasswordButton = render.makeButton("Change Password", "changePasswordActionButton", "b", "ui-btn-icon-left ui-icon-check");
			var backButton = render.makeBackButton("Close", "cancelChangePasswordButton", "a");
			var buttonBar = render.makeHorzControlGroup(changePasswordButton + backButton);

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

			util.setHtmlEnhanced($("#changePasswordPg"), content);

			$("#changePasswordActionButton").on("click", user.changePassword);
		},
		
		init : function() {
			util.delayedFocus("#changePassword1");
		}
	};

	console.log("Module ready: changePasswordPg.js");
	return _;
}();

// # sourceURL=changePasswordPg.js
