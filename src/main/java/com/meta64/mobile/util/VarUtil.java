package com.meta64.mobile.util;

public class VarUtil {
	public static boolean safeBooleanVal(Boolean val) {
		return val != null && val.booleanValue();
	}
}
