package com.meta64.mobile.config;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.repo.OakRepositoryBean;

/**
 * Wrapper for holding variables that we need to maintain server state of for a specific session.
 * Basic session state storage is all collected here.
 */
@Component
@Scope("session")
public class SessionContext {
	private String userName;
	private String password;
	private String captcha;
	private String timezone;

	/* Initial id param parsed from first URL request */
	private String urlId;

	public int counter;

	@Autowired
	private OakRepositoryBean oak;

	@PreDestroy
	public void preDestroy() {
		// not used currently
	}

	public boolean isAdmin() {
		return "admin".equalsIgnoreCase(userName);
	}

	/*
	 * This can create nasty bugs. I should bet always getting user name from the actual session
	 * object itself in all the logic... in most every case except maybe login process.
	 */
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

	public String getCaptcha() {
		return captcha;
	}

	public void setCaptcha(String captcha) {
		this.captcha = captcha;
	}

	public String getUrlId() {
		return urlId;
	}

	public void setUrlId(String urlId) {
		this.urlId = urlId;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}
}
