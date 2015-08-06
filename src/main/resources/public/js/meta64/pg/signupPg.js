console.log("running module: signupPg.js");

var signupPg = function() {

	var _ = {
		domId : "signupPg",

		build : function() {

			var header = render.makeTag("div", //
			{
				"data-role" : "header"// ,
			// "data-position" : "fixed",
			// "data-tap-toggle" : "false"
			}, //
			"<h2>" + BRANDING_TITLE + " - Signup</h2>");

			var formControls = render.makeEditField("User", "signupUserName") + //
			render.makePasswordField("Password", "signupPassword") + //
			render.makeEditField("Email", "signupEmail") + //
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

			var signupButton = render.makeButton("Signup", "signupButton", "b", "ui-btn-icon-left ui-icon-check");
			var newCaptchaButton = render.makeButton("Try Different Image", "tryAnotherCaptchaButton", "a");
			var backButton = render.makeBackButton("Close", "cancelSignupButton", "a");

			var buttonBar = render.makeHorzControlGroup(signupButton + newCaptchaButton + backButton);

			var form = render.makeTag("div", //
			{
				"class" : "ui-field-contain" //
			}, //
			formControls + captchaImage + buttonBar);

			var internalMainContent = ""; // Note: No email address is
			// required currently, because this
			// app is an alpha site that doesn't
			// yet have email support.";

			var mainContent = render.makeTag("div", //
			{
				"role" : "main",
				"class" : "ui-content dialog-content", 
				"id" : _.domId + "-main"
			}, //
			internalMainContent + form);

			var content = header + mainContent;

			util.setHtmlEnhanced($("#signupPg"), content);

			/* 
			$("#" + _.domId + "-main").css({
				"backgroundImage" : "url(/ibm-702-bright.jpg);" 
					"background-repeat" : "no-repeat;",
					"background-size" : "100% auto"
			});
			*/
			
			$("#tryAnotherCaptchaButton").on("click", user.tryAnotherCaptcha);
			$("#signupButton").on("click", user.signup);
		},

		init : function() {
			user.pageInitSignupPg();
			util.delayedFocus("#signupUserName");
		}
	};

	console.log("Module ready: signupPg.js");
	return _;
}();

// # sourceURL=signupPg.js
