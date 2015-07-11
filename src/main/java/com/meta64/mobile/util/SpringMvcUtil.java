package com.meta64.mobile.util;

import org.springframework.ui.Model;

public class SpringMvcUtil {

	public static void addJsFileNameProps(Model model, String jsVer, String... fileNames) {
		for (String fileName : fileNames) {
			model.addAttribute(fileName + "Js", "/js/meta64/" + fileName + ".js?jsVer=" + jsVer);
		}
	}
}
