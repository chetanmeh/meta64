package com.meta64.mobile.util;

import org.springframework.ui.Model;

public class SpringMvcUtil {

	public static void addJsFileNameProps(Model model, String ver, String... fileNames) {
		for (String fileName : fileNames) {
			model.addAttribute(fileName + "Js", "/js/meta64/" + fileName + ".js?ver=" + ver);
		}
	}

	public static void addCssFileNameProps(Model model, String ver, String... fileNames) {
		for (String fileName : fileNames) {
			model.addAttribute(fileName + "Css", "../css/" + fileName + ".css?ver=" + ver);
		}
	}
}
