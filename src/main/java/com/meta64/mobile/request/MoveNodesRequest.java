package com.meta64.mobile.request;

import java.util.List;

public class MoveNodesRequest {
	/* parent under which the nodes will be moved */
	private String targetNodeId;

	/*
	 * optional (can be null), when provided as non-null, this means not only do we have
	 * 'targetNodeId' specifying the parent node for the paste but we also have the targetChildId,
	 * which is the node at the ordinal position that we want to have the nodes being moved end up
	 * at. So the nodes getting moved will be all put in at the targetChildId node position and all
	 * the existing child nodes will be moved down enough slots to make room for the nodes being
	 * moved in.
	 */
	private String targetChildId;

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

	public String getTargetChildId() {
		return targetChildId;
	}

	public void setTargetChildId(String targetChildId) {
		this.targetChildId = targetChildId;
	}
}
