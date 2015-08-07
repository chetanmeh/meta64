package com.meta64.mobile.config;

/**
 * Part of infrastucture for injecting from properties files ultimately onto Thymeleaf generated
 * HTML. This is part of the mechanism of how we get settings that originat in server side
 * properties files to be used in the browser, without needing to do an Ajax call to retrieve it.
 */
public interface ConstantsProvider {

	public String getCookiePrefix();

	public String getBrandingTitle();

	public String getCacheVersion();

	public String getProfileName();

	public String getHostAndPort();
}
