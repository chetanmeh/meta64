package com.meta64.mobile.response;

import com.meta64.mobile.model.NodeInfo;
import com.meta64.mobile.response.base.OakResponseBase;

public class InitNodeEditResponse extends OakResponseBase {
	private NodeInfo nodeInfo;

	public NodeInfo getNodeInfo() {
		return nodeInfo;
	}

	public void setNodeInfo(NodeInfo nodeInfo) {
		this.nodeInfo = nodeInfo;
	}
}
