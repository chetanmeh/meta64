package com.meta64.mobile.model;

public class UserPreferences {
	private boolean advancedMode;
	private String lastNode;

	public boolean isAdvancedMode() {
		return advancedMode;
	}

	public void setAdvancedMode(boolean advancedMode) {
		this.advancedMode = advancedMode;
	}

	public String getLastNode() {
		return lastNode;
	}

	public void setLastNode(String lastNode) {
		this.lastNode = lastNode;
	}
}
