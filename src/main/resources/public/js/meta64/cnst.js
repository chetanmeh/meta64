console.log("running module: cnst.js");

var cnst = function() {

	var _ = {
		ANON : "anonymous",
		COOKIE_LOGIN_USR : cookiePrefix + "loginUsr",
		COOKIE_LOGIN_PWD : cookiePrefix + "loginPwd",
		/*
		 * loginState="0" if user logged out intentionally.
		 * loginState="1" if last known state of user was 'logged in'
		 */
		COOKIE_LOGIN_STATE : cookiePrefix + "loginState",
		BR : "<div class='vert-space'></div>",
		INSERT_ATTACHMENT : "{{insert-attachment}}",
		NEW_ON_TOOLBAR : false,
		INS_ON_TOOLBAR : false
	};

	console.log("Module ready: cnst.js");
	return _;
}();

//# sourceURL=cnst.js
