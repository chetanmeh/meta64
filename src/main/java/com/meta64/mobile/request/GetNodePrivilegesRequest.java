package com.meta64.mobile.request;

public class GetNodePrivilegesRequest {
	private String nodeId;
	private boolean includeAcl;
	private boolean includeOwners;
	
	public boolean isIncludeAcl() {	
		return includeAcl;
	}

	public void setIncludeAcl(boolean includeAcl) {
		this.includeAcl = includeAcl;
	}

	public boolean isIncludeOwners() {
		return includeOwners;
	}

	public void setIncludeOwners(boolean includeOwners) {
		this.includeOwners = includeOwners;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
}
