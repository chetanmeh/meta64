package com.meta64.mobile.config;

public class AppConstant {

	public static final String NAMESPACE = "nt";

	public static final String JCR_PROP_BIN = NAMESPACE + ":bin";
	public static final String JCR_PROP_BIN_VER = NAMESPACE + ":binVer";
	
	/* I want to use jcr namespace for these since they exist */
	public static final String JCR_PROP_BIN_DATA = "jcr:data";
	public static final String JCR_PROP_BIN_MIME = "jcr:mimeType";
	
	public static final String JCR_PROP_IMG_WIDTH = NAMESPACE + ":imgWidth";
	public static final String JCR_PROP_IMG_HEIGHT = NAMESPACE + ":imgHeight";
}
