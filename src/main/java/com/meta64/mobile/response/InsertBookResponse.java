package com.meta64.mobile.response;

import com.meta64.mobile.model.NodeInfo;
import com.meta64.mobile.response.base.OakResponseBase;

public class InsertBookResponse extends OakResponseBase {
	private NodeInfo newNode;

	public NodeInfo getNewNode() {
		return newNode;
	}

	public void setNewNode(NodeInfo newNode) {
		this.newNode = newNode;
	}
}
