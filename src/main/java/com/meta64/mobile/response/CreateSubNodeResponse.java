package com.meta64.mobile.response;

import com.meta64.mobile.model.NodeInfo;
import com.meta64.mobile.response.base.OakResponseBase;

public class CreateSubNodeResponse extends OakResponseBase {
	// private String newChildNodeId;
	private NodeInfo newNode;

	// public String getNewChildNodeId() {
	// return newChildNodeId;
	// }
	//
	// public void setNewChildNodeId(String newChildNodeId) {
	// this.newChildNodeId = newChildNodeId;
	// }

	public NodeInfo getNewNode() {
		return newNode;
	}

	public void setNewNode(NodeInfo newNode) {
		this.newNode = newNode;
	}

}
