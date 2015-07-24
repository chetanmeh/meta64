package com.meta64.mobile.request;

public class LoginRequest {
	private String userName;
	private String password;
	private boolean usingCookies;

	/* timezone offset */
	private int tzOffset;

	/* daylight savings time */
	private boolean dst;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isUsingCookies() {
		return usingCookies;
	}

	public void setUsingCookies(boolean usingCookies) {
		this.usingCookies = usingCookies;
	}

	public int getTzOffset() {
		return tzOffset;
	}

	public void setTzOffset(int tzOffset) {
		this.tzOffset = tzOffset;
	}

	public boolean isDst() {
		return dst;
	}

	public void setDst(boolean dst) {
		this.dst = dst;
	}
}
