console.log("running module: cnst.js");

var cnst = function() {

	var _ = {
		ANON : "anonymous",
		COOKIE_LOGIN_USR : cookiePrefix + "loginUsr",
		COOKIE_LOGIN_PWD : cookiePrefix + "loginPwd",
		BR : "<div class='vert-space'></div>",
		INSERT_ATTACHMENT : "{{insert-attachment}}"
	};

	console.log("Module ready: cnst.js");
	return _;
}();

//# sourceURL=cnst.js
