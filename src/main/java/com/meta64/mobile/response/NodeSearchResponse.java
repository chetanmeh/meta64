package com.meta64.mobile.response;

import java.util.List;

import com.meta64.mobile.model.NodeInfo;
import com.meta64.mobile.response.base.OakResponseBase;

public class NodeSearchResponse extends OakResponseBase {

	/* orderablility of children not set in these objects, all will be false */
	private List<NodeInfo> searchResults;

	public List<NodeInfo> getSearchResults() {
		return searchResults;
	}

	public void setSearchResults(List<NodeInfo> searchResults) {
		this.searchResults = searchResults;
	}
}
