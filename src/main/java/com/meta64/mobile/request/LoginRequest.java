package com.meta64.mobile.request;

public class LoginRequest {
	private String userName;
	private String password;

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
