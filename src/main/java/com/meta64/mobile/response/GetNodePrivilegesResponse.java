package com.meta64.mobile.response;

import java.util.List;

import com.meta64.mobile.model.AccessControlEntryInfo;
import com.meta64.mobile.response.base.OakResponseBase;

public class GetNodePrivilegesResponse extends OakResponseBase {
	private List<AccessControlEntryInfo> aclEntries;

	public List<AccessControlEntryInfo> getAclEntries() {
		return aclEntries;
	}

	public void setAclEntries(List<AccessControlEntryInfo> aclEntries) {
		this.aclEntries = aclEntries;
	}
}
