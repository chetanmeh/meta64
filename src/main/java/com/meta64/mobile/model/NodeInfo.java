package com.meta64.mobile.model;

import java.util.List;

/**
 * Primary object passed back to client to represent a 'node'. Client sees the JSON version of this,
 * in javascript.
 */
public class NodeInfo {

	private String id;
	private String path;
	private String name;
	private String primaryTypeName;

	// TODO: There is a slight waste of space here, because createdBy is also in 'properties' but so
	// the client can
	// have super fast access to it, i'm making it a direct property also.
	private String createdBy;
	private String lastModified;

	private List<PropertyInfo> properties;
	private boolean hasChildren;
	private boolean hasBinary;
	private boolean binaryIsImage;
	private long binVer;
	private int width;
	private int height;

	/* true if parent is "orderable" node */
	private boolean childrenOrdered;

	public NodeInfo() {
	}

	public NodeInfo(String id, String path, String name, List<PropertyInfo> properties, boolean hasChildren, boolean childrenOrdered, boolean hasBinary,
			boolean binaryIsImage, long binVer, int width, int height, String createdBy, String lastModified, String primaryTypeName) {
		this.id = id;
		this.path = path;
		this.name = name;
		this.properties = properties;
		this.hasChildren = hasChildren;
		this.childrenOrdered = childrenOrdered;
		this.hasBinary = hasBinary;
		this.binaryIsImage = binaryIsImage;
		this.binVer = binVer;
		this.width = width;
		this.height = height;
		this.createdBy = createdBy;
		this.lastModified = lastModified;
		this.primaryTypeName = primaryTypeName;
	}

	public boolean isChildrenOrdered() {
		return childrenOrdered;
	}

	public void setChildrenOrdered(boolean childrenOrdered) {
		this.childrenOrdered = childrenOrdered;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<PropertyInfo> getProperties() {
		return properties;
	}

	public void setProperties(List<PropertyInfo> properties) {
		this.properties = properties;
	}

	public boolean isHasChildren() {
		return hasChildren;
	}

	public void setHasChildren(boolean hasChildren) {
		this.hasChildren = hasChildren;
	}

	public boolean isHasBinary() {
		return hasBinary;
	}

	public void setHasBinary(boolean hasBinary) {
		this.hasBinary = hasBinary;
	}

	public boolean isBinaryIsImage() {
		return binaryIsImage;
	}

	public void setBinaryIsImage(boolean binaryIsImage) {
		this.binaryIsImage = binaryIsImage;
	}

	public long getBinVer() {
		return binVer;
	}

	public void setBinVer(long binVer) {
		this.binVer = binVer;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getLastModified() {
		return lastModified;
	}

	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}

	public String getPrimaryTypeName() {
		return primaryTypeName;
	}

	public void setPrimaryTypeName(String primaryTypeName) {
		this.primaryTypeName = primaryTypeName;
	}
}
