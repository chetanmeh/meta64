package com.meta64.mobile.request;

public class ImportRequest {
	private String nodeId;

	/*
	 * short file name (i.e. not including folder or extension) of target file to be imported from. It's expected to be in the folder specified by adminDataFolder
	 * application property. 
	 */
	private String sourceFileName;

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getSourceFileName() {
		return sourceFileName;
	}

	public void setSourceFileName(String sourceFileName) {
		this.sourceFileName = sourceFileName;
	}
}
