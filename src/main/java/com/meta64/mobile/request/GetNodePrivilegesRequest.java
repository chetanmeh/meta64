package com.meta64.mobile.request;

public class GetNodePrivilegesRequest {
	private String nodeId;
	private String includeAcl;
	private String includeOwners;

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getIncludeAcl() {
		return includeAcl;
	}

	public void setIncludeAcl(String includeAcl) {
		this.includeAcl = includeAcl;
	}

	public String getIncludeOwners() {
		return includeOwners;
	}

	public void setIncludeOwners(String includeOwners) {
		this.includeOwners = includeOwners;
	}
}
