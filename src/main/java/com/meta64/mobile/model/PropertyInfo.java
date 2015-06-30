package com.meta64.mobile.model;

import java.util.List;

public class PropertyInfo {
	private int type;
	private String name;

	/* Only one of these will be non-null. The property is either multi-valued or single valued */
	private String value;
	private List<String> values;

	public PropertyInfo() {
	}

	public PropertyInfo(int type, String name, String value, List<String> values) {
		this.type = type;
		this.name = name;
		this.value = value;
		this.values = values;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}
}
