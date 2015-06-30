package com.meta64.mobile.request;

public class RemovePrivilegeRequest {

	private String nodeId;

	private String principal;

	/* for now only 'public' is the only option we support */
	private String privilege;

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

	public String getPrivilege() {
		return privilege;
	}

	public void setPrivilege(String privilege) {
		this.privilege = privilege;
	}
}
