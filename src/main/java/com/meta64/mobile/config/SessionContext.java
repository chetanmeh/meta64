package com.meta64.mobile.config;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.model.RefInfo;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.util.DateUtil;

/**
 * Wrapper for holding variables that we need to maintain server state of for a specific session.
 * Basic session state storage is all collected here.
 */
@Component
@Scope("session")
public class SessionContext {
	private ReentrantLock lock = new ReentrantLock();
	
	/* Identification of user's account root node */
	private RefInfo rootRefInfo;
	
	private String userName;
	private String password;
	private String captcha;
	private String timezone;
	private String timeZoneAbbrev;

	/*
	 * For extreme scalability we 'could' hold a formatter in a blobal map where there is basically
	 * one fomratter per timezone/DST combination, but until we have hundreds of concurrent users
	 * this won't be an issue. Having one date format per session should really never be a
	 * scalability issue but there nonetheless *is* very slight wasted memory here.
	 */
	private SimpleDateFormat dateFormat;

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
	
	public String formatTime(Date date) {

		/* If we have a short timezone abbreviation display timezone with it */
		if (getTimeZoneAbbrev() != null) {
			if (dateFormat == null) {
				dateFormat = new SimpleDateFormat(DateUtil.DATE_FORMAT_NO_TIMEZONE, DateUtil.DATE_FORMAT_LOCALE);
				dateFormat.setTimeZone(TimeZone.getTimeZone(getTimezone()));
			}
			return dateFormat.format(date) + " " + getTimeZoneAbbrev();
		}
		/* else display timezone in standard GMT fomrat */
		else {
			if (dateFormat == null) {
				dateFormat = new SimpleDateFormat(DateUtil.DATE_FORMAT_WITH_TIMEZONE, DateUtil.DATE_FORMAT_LOCALE);
				dateFormat.setTimeZone(TimeZone.getTimeZone(getTimezone()));
			}
			return dateFormat.format(date);
		}
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

	public String getTimeZoneAbbrev() {
		return timeZoneAbbrev;
	}

	public void setTimeZoneAbbrev(String timeZoneAbbrev) {
		this.timeZoneAbbrev = timeZoneAbbrev;
	}

	public RefInfo getRootRefInfo() {
		return rootRefInfo;
	}

	public void setRootRefInfo(RefInfo rootRefInfo) {
		this.rootRefInfo = rootRefInfo;
	}

	public ReentrantLock getLock() {
		return lock;
	}

	public void setLock(ReentrantLock lock) {
		this.lock = lock;
	}
}
