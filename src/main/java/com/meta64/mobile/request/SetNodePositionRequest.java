package com.meta64.mobile.request;

/* 
 * Moves a nodeId to be before siblingId. Setting siblingId null or empty moves nodeId to end of list 
 */
public class SetNodePositionRequest {
	private String parentNodeId;
	private String nodeId;
	private String siblingId;

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getSiblingId() {
		return siblingId;
	}

	public void setSiblingId(String siblingId) {
		this.siblingId = siblingId;
	}

	public String getParentNodeId() {
		return parentNodeId;
	}

	public void setParentNodeId(String parentNodeId) {
		this.parentNodeId = parentNodeId;
	}
}
