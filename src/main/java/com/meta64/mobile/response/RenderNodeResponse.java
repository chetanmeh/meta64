package com.meta64.mobile.response;

import java.util.List;

import com.meta64.mobile.model.NodeInfo;
import com.meta64.mobile.response.base.OakResponseBase;

public class RenderNodeResponse extends OakResponseBase {

	/* child ordering flag is set in this node object and is correct */
	private NodeInfo node;

	/* orderablility of children not set in these objects, all will be false */
	private List<NodeInfo> children;

	/*
	 * under certain situations the rendering request will be pointed to parent node instead and we
	 * send back to the client 'true' here when that happens.
	 */
	private boolean displayedParent;

	public List<NodeInfo> getChildren() {
		return children;
	}

	public void setChildren(List<NodeInfo> children) {
		this.children = children;
	}

	public NodeInfo getNode() {
		return node;
	}

	public void setNode(NodeInfo node) {
		this.node = node;
	}

	public boolean isDisplayedParent() {
		return displayedParent;
	}

	public void setDisplayedParent(boolean displayedParent) {
		this.displayedParent = displayedParent;
	}
}
