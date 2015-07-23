package com.meta64.mobile.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
@Scope("singleton")
public class SpringMvcUtil {

	@Value("${jqueryMobileCss}")
	private String jqueryMobileCss;
	
	@Value("${jqueryJs}")
	private String jqueryJs;
	
	@Value("${jqueryMobileJs}")
	private String jqueryMobileJs;
	
	public void addJsFileNameProps(Model model, String ver, String... fileNames) {
		for (String fileName : fileNames) {
			model.addAttribute(fileName + "Js", "/js/meta64/" + fileName + ".js?ver=" + ver);
		}
	}

	public void addCssFileNameProps(Model model, String ver, String... fileNames) {
		for (String fileName : fileNames) {
			model.addAttribute(fileName + "Css", "../css/" + fileName + ".css?ver=" + ver);
		}
	}
	
	public void addThirdPartyLibs(Model model) {
		model.addAttribute("jqueryMobileCss", jqueryMobileCss);
		model.addAttribute("jqueryJs", jqueryJs);
		model.addAttribute("jqueryMobileJs", jqueryMobileJs);
	}
}
