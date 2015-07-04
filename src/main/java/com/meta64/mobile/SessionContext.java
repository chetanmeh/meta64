package com.meta64.mobile;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Wrapper for holding variables that we do need to maintain server state of for a specific session.
 * Basic session state storage is all collected here.
 */
@Component
@Scope("session")
public class SessionContext {
	private String userName;
	private String password;
	private String captcha;

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
}
