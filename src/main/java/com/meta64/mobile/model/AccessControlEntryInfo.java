package com.meta64.mobile.model;

import java.util.List;

/**
 * Represents a certain principle and a set of privileges the principle has.
 */
public class AccessControlEntryInfo {
	private String principalName;

	private List<PrivilegeInfo> privileges;

	public String getPrincipalName() {
		return principalName;
	}

	public void setPrincipalName(String principalName) {
		this.principalName = principalName;
	}

	public List<PrivilegeInfo> getPrivileges() {
		return privileges;
	}

	public void setPrivileges(List<PrivilegeInfo> privileges) {
		this.privileges = privileges;
	}
}
