package com.meta64.mobile.request;

public class LoginRequest {
	private String userName;
	private String password;
	private boolean usingCookies;

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
}
