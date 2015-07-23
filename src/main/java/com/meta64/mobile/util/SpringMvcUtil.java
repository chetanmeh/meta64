package com.meta64.mobile.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import com.meta64.mobile.config.ConstantsProviderImpl;

@Component
@Scope("singleton")
public class SpringMvcUtil {

	@Value("${jqueryMobileCss}")
	private String jqueryMobileCss;

	@Value("${jqueryJs}")
	private String jqueryJs;

	@Value("${jqueryMobileJs}")
	private String jqueryMobileJs;

	/*
	 * Each time the server restarts we have a new version number here and will cause clients to
	 * download new version of JS files into their local browser cache. For now the assumption is
	 * that this is better then having to remember to update version numbers to invalidate client
	 * caches, but in production systems we may not want to push new JS just because of a server
	 * restart so this will change in the future. That is, the 'currentTimeMillis' part will change
	 * to some kind of an actual version number or something, that will be part of managed releases.
	 */
	public static final long jsVersion = System.currentTimeMillis();
	public static final long cssVersion = jsVersion; // match jsVersion for now, why not.

	/*
	 * This is an acceptable hack to reference the Impl class directly like this.
	 */
	static {
		ConstantsProviderImpl.setCacheVersion(String.valueOf(jsVersion));
	}

	public void addJsFileNameProp(Model model, String varName, String fileName) {
		model.addAttribute(varName, fileName + ".js?ver=" + String.valueOf(jsVersion));
	}

	public void addCssFileNameProp(Model model, String varName, String fileName) {
		model.addAttribute(varName, fileName + ".css?ver=" + String.valueOf(jsVersion));
	}

	public void addThirdPartyLibs(Model model) {
		model.addAttribute("jqueryMobileCss", jqueryMobileCss);
		model.addAttribute("jqueryJs", jqueryJs);
		model.addAttribute("jqueryMobileJs", jqueryMobileJs);
	}
}
