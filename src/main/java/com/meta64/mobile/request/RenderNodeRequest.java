package com.meta64.mobile.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RenderNodeRequest {

	/* can be node id or path. server interprets correctly no matter which */
	private String nodeId;

	/*
	 * holds number of levels to move up the parent chain from 'nodeId' before rendering, or zero to
	 * render at nodeId itself
	 */
	private int upLevel;
	private boolean renderParentIfLeaf;

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public int getUpLevel() {
		return upLevel;
	}

	public void setUpLevel(int upLevel) {
		this.upLevel = upLevel;
	}

	@JsonProperty(required = false)
	public boolean isRenderParentIfLeaf() {
		return renderParentIfLeaf;
	}

	public void setRenderParentIfLeaf(boolean renderParentIfLeaf) {
		this.renderParentIfLeaf = renderParentIfLeaf;
	}
}
