package com.meta64.mobile.response;

import com.meta64.mobile.model.RefInfo;
import com.meta64.mobile.model.UserPreferences;
import com.meta64.mobile.response.base.OakResponseBase;

public class LoginResponse extends OakResponseBase {

	private RefInfo rootNode;

	/* will be username or 'anonymous' if server rejected login */
	private String userName;
	
	private String anonUserLandingPageNode;
	
	/* we can optionally send back something here to force the client to load the specified node instead 
	 * of whatever other node it would have loaded for whatever series of reasons. This is a hard override for 
	 * anything else.
	 */
	private String homeNodeOverride;
	
	private UserPreferences userPreferences;

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

	public String getHomeNodeOverride() {
		return homeNodeOverride;
	}

	public void setHomeNodeOverride(String homeNodeOverride) {
		this.homeNodeOverride = homeNodeOverride;
	}

	public UserPreferences getUserPreferences() {
		return userPreferences;
	}

	public void setUserPreferences(UserPreferences userPreferences) {
		this.userPreferences = userPreferences;
	}
}
