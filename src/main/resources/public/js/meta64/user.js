console.log("running module: user.js");

var user = function() {

	/* ret is LoginResponse.java */
	var _loginResponse = function(res) {
		//console.log("Login.success=" + JSON.stringify(res));
		if (util.checkSuccess("Login", res)) {
			$.mobile.changePage($('#mainPage'), 'pop', false, true);
			
			meta64.homeNodeId = res.rootNode.id;
			meta64.homeNodePath = res.rootNode.path;
			
			meta64.isAdminUser = res.userName === "admin";
			meta64.isAnonUser = res.userName === "anonymous";
			
			view.refreshTree(meta64.homeNodeId);
			$("#headerUserName").html("Meta64 - User: " + res.userName);
		} 
	}

	var _changePasswordResponse = function(res) {
		if (util.checkSuccess("Change password", res)) {
			alert("Password changed successfully.");
		}
	}

	var _signupResponse = function(res) {
		if (util.checkSuccess("Signup new user", res)) {
			$.mobile.changePage("#loginDialogId");
			alert("Signup successful.");
		}
	}

	var _ = {
		signup : function() {
			var userName = util.getRequiredElement("#signupUserName").val();
			var password = util.getRequiredElement("#signupPassword").val();
			var email = util.getRequiredElement("#signupEmail").val();
			var captcha = util.getRequiredElement("#signupCaptcha").val();

			/* no real validation yet, other than non-empty */
			if (util.anyEmpty(userName, password, email, captcha)) {
				alert('Sorry, you cannot leave any fields blank.');
				return;
			}

			util.json("signup", {
				"userName" : userName,
				"password" : password,
				"email" : email,
				"captcha" : captcha
			}, _signupResponse);
		},

		pageInitSignupDialog : function() {
			user.tryAnotherCaptcha();
		},

		tryAnotherCaptcha : function() {

			var n = util.currentTimeMillis();

			/*
			 * embed a time parameter just to thwart browser caching, and ensure
			 * server and browser will never return the same image twice.
			 */
			var src = "/mobile/rest/captcha?t=" + n
			// console.log("Setting captcha image src: "+src);

			/*
			 * we use a semi-guaranteed unique (timecodebased) integer to force
			 * a new captcha to be returned
			 */
			$("#captchaImage").attr("src", src);
		},

		login : function() {
			if (!util.isActionEnabled("login")) {
				return;
			}

			var userNameVal = $.trim($("#userName").val());
			var passwordVal = $.trim($("#password").val());

			util.json("login", {
				"userName" : userNameVal,
				"password" : passwordVal
			}, _loginResponse);
		},

		changePassword : function() {
			var pwd1 = util.getRequiredElement("#changePassword1").val();
			var pwd2 = util.getRequiredElement("#changePassword2").val();
			if (pwd1 && pwd1.length >= 4 && pwd1 === pwd2) {
				util.json("changePassword", {
					"newPassword" : pwd1
				}, _changePasswordResponse);
			} else {
				alert('Sorry, invalid password(s).');
			}
		},

		changePasswordDialog : function() {
			$.mobile.changePage("#changePasswordDialog");
		},

		updateLoginButton : function(enablement) {
			if (enablement.userName === "anonymous") {
				$("#loginLogoutButton").text("Login");
			} else {
				$("#loginLogoutButton").text("Logout");
			}
		}
	};

	console.log("Module ready: user.js");
	return _;
}();