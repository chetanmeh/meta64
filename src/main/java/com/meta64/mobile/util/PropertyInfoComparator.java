package com.meta64.mobile.util;

import java.util.Comparator;

import com.meta64.mobile.model.PropertyInfo;

class PropertyInfoComparator implements Comparator<PropertyInfo> {
	public int compare(PropertyInfo a, PropertyInfo b) {
		return a.getName().compareTo(b.getName());
	}
}