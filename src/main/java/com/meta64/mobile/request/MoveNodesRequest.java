package com.meta64.mobile.request;

import java.util.List;

public class MoveNodesRequest {
	/* for now this targetId is the parent under which the nodes will be moved */
	private String targetNodeId;
	
	private List<String> nodeIds;

	public List<String> getNodeIds() {
		return nodeIds;
	}

	public void setNodeIds(List<String> nodeIds) {
		this.nodeIds = nodeIds;
	}

	public String getTargetNodeId() {
		return targetNodeId;
	}

	public void setTargetNodeId(String targetNodeId) {
		this.targetNodeId = targetNodeId;
	}
}
