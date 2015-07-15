console.log("running module: constant.js");

var constant = function() {
	
	var _ = {
		ANON : "anonymous"
	};

	console.log("Module ready: constant.js");
	return _;
}();
