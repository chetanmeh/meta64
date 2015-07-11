package com.meta64.mobile.request;

public class AnonPageLoadRequest {
	private boolean ignoreUrl;
	private String urlQuery;
	
	public boolean isIgnoreUrl() {
		return ignoreUrl;
	}

	public void setIgnoreUrl(boolean ignoreUrl) {
		this.ignoreUrl = ignoreUrl;
	}

	public String getUrlQuery() {
		return urlQuery;
	}

	public void setUrlQuery(String urlQuery) {
		this.urlQuery = urlQuery;
	}
}
