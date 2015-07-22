console.log("running module: signupPg.js");

var signupPg = function() {

	var _ = {
			build : function() {

				var header = render.makeTag("div", //
				{
					"data-role" : "header" //
				}, //
				"<h2>" + BRANDING_TITLE + " - Signup</h2>");

				var formControls = render.makeEditField("User", "signupUserName") + //
				render.makePasswordField("Password", "signupPassword") + //
				render.makeEditField("Captcha", "signupCaptcha");

				var captchaImage = render.makeTag("div", //
				{
					"class" : "captcha-image" //
				}, //
				render.makeTag("img", //
				{
					"id" : "captchaImage",
					"class" : "captcha",
					"src" : ""//
				}, //
				"", false));

				var signupButton = render.makeButton("Signup", "signupButton", "b");
				var newCaptchaButton = render.makeButton("Try Different Image", "tryAnotherCaptchaButton", "a");
				var backButton = render.makeBackButton("Close", "cancelSignupButton", "a");

				var buttonBar = render.makeHorzControlGroup(signupButton + newCaptchaButton + backButton);

				var form = render.makeTag("div", //
				{
					"class" : "ui-field-contain" //
				}, //
				formControls + captchaImage + buttonBar);

				var internalMainContent = "Note: No email address is required currently, because this app is an alpha site that doesn't yet have email support.";

				var mainContent = render.makeTag("div", //
				{
					"role" : "main", //
					"class" : "ui-content"
				}, //
				internalMainContent + form);

				var content = header + mainContent;

				util.setHtmlEnhanced($("#signupPg"), content);

				$("#tryAnotherCaptchaButton").on("click", _.tryAnotherCaptcha);
				$("#signupButton").on("click", _.signup);
			}
	};

	console.log("Module ready: signupPg.js");
	return _;
}();

// # sourceURL=signupPg.js
