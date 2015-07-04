console.log("running module: search.js");

var search = function() {

	/*
	 * ================= PRIVATE =================
	 */

	/*
	 * ================= PUBLIC =================
	 */
	var _ = {
		searchNodes : function() {
			alert('searchNodes!');
		},
		
		searchNodesDialog : function() {
			$.mobile.changePage("#searchNodesDialog");
		}
	}

	console.log("Module ready: search.js");
	return _;
}();