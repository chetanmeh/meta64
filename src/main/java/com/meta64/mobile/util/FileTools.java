package com.meta64.mobile.util;

import java.io.File;

public class FileTools {

	public static boolean fileExists(String fileName) {
		if (fileName == null || fileName.equals("")) return false;

		return new File(fileName).isFile();
	}

	public static boolean dirExists(String fileName) {
		if (fileName == null || fileName.equals("")) return false;

		return new File(fileName).isDirectory();
	}
}
