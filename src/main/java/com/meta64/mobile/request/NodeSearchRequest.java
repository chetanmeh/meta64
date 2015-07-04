package com.meta64.mobile.request;

public class NodeSearchRequest {

	/* can be node id or path. server interprets correctly no matter which */
	private String nodeId;

	private String searchText;

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}
}
