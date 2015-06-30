package com.meta64.mobile.response;

import com.meta64.mobile.response.base.OakResponseBase;

public class AnonPageLoadResponse extends OakResponseBase {
	private String content;
	private RenderNodeResponse renderNodeResponse;

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public RenderNodeResponse getRenderNodeResponse() {
		return renderNodeResponse;
	}

	public void setRenderNodeResponse(RenderNodeResponse renderNodeResponse) {
		this.renderNodeResponse = renderNodeResponse;
	}
}
