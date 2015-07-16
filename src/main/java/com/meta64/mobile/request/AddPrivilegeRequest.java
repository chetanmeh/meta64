package com.meta64.mobile.request;

import java.util.List;

public class AddPrivilegeRequest {

	private String nodeId;

	/* for now only 'public' is the only option we support */
	private List<String> privileges;

	private String principal;

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getPrincipal() {
		return principal;
	}

	public void setPrincipal(String principal) {
		this.principal = principal;
	}

	public List<String> getPrivileges() {
		return privileges;
	}

	public void setPrivileges(List<String> privileges) {
		this.privileges = privileges;
	}
}
