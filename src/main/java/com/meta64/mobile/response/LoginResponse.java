package com.meta64.mobile.response;

import com.meta64.mobile.model.RefInfo;
import com.meta64.mobile.response.base.OakResponseBase;

public class LoginResponse extends OakResponseBase {

	private RefInfo rootNode;

	/* will be username or 'anonymous' if server rejected login */
	private String userName;
	
	private String anonUserLandingPageNode;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public RefInfo getRootNode() {
		return rootNode;
	}

	public void setRootNode(RefInfo rootNode) {
		this.rootNode = rootNode;
	}

	public String getAnonUserLandingPageNode() {
		return anonUserLandingPageNode;
	}

	public void setAnonUserLandingPageNode(String anonUserLandingPageNode) {
		this.anonUserLandingPageNode = anonUserLandingPageNode;
	}
}
