console.log("running module: cnst.js");

var cnst = function() {
	
	var _ = {
		ANON : "anonymous",
		COOKIE_LOGIN_USR : cookiePrefix+"loginUsr",
		COOKIE_LOGIN_PWD : cookiePrefix+"loginPwd"
	};

	console.log("Module ready: cnst.js");
	return _;
}();
