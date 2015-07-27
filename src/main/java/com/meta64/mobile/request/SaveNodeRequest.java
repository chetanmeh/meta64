package com.meta64.mobile.request;

import java.util.List;

import com.meta64.mobile.model.PropertyInfo;

public class SaveNodeRequest {
	private String nodeId;

	/*
	 * properties to save. Not necessarily the complete list of properties on this node, but just
	 * the ones we will persist
	 */
	private List<PropertyInfo> properties;
	
	private boolean sendNotification;

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public List<PropertyInfo> getProperties() {
		return properties;
	}

	public void setProperties(List<PropertyInfo> properties) {
		this.properties = properties;
	}

	public boolean isSendNotification() {
		return sendNotification;
	}

	public void setSendNotification(boolean sendNotification) {
		this.sendNotification = sendNotification;
	}
}
