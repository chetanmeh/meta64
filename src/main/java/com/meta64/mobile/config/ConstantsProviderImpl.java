package com.meta64.mobile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * I'm using this class to inject strings into the HTML using thymleaf and using properties file as
 * the source of the strings to inject. There is a way to inject directly from a properties file
 * into thymleaf, but it looks more complex and less powerful than this approach. Using the
 * constantsProvider we get access to properties in a way were we can actually process them if we
 * need to before handing them to spring, because we are implementing the getters here.
 */
@Component("constantsProvider")
@Scope("singleton")
public class ConstantsProviderImpl implements ConstantsProvider {

	@Value("${metaHost}")
	private String metaHost;

	@Value("${server.port}")
	private String serverPort;

	@Value("${metaRestPath}")
	private String metaRestPath;
	
	@Value("${cookiePrefix}")
	private String cookiePrefix;

	@Value("${brandingTitle}")
	private String brandingTitle;
	
	@Override
	public String getRestUrl() {
		return "http://" + metaHost + ":" + serverPort + metaRestPath;
	}
	
	@Override
	public String getCookiePrefix() {
		return cookiePrefix;
	}
	
	@Override
	public String getBrandingTitle() {
		return brandingTitle;
	}
}
