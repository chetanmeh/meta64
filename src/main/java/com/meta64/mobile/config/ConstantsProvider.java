package com.meta64.mobile.config;

/**
 * Part of infrastucture for injecting from properties files ultimately onto Thymeleaf generated
 * HTML
 */
public interface ConstantsProvider {
	public String getApiUrl();

	public String getCookiePrefix();

	public String getBrandingTitle();

	public String getCacheVersion();

	public String getProfileName();

	public String getHostAndPort();
}
