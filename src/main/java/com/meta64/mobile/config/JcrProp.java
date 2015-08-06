package com.meta64.mobile.config;

/*
 * JCR Node Property Names
 */
public class JcrProp {

	public static final String MIXIN_TYPES = "jcr:mixinTypes";

	public static final String USER_PREF_LAST_NODE = "lastNode";
	public static final String USER_PREF_ADV_MODE = "advMode";

	/*
	 * Goes on node: Paths defined by: JcrUtil.getSystemOutbox(session) + GUID (guid is unique per
	 * outbound email)
	 */
	public static final String EMAIL_CONTENT = "jcr:content";
	public static final String EMAIL_RECIP = "recip";
	public static final String EMAIL_SUBJECT = "subject";

	public static final String CREATED = "jcr:created";
	public static final String CREATED_BY = "jcr:createdBy";
	public static final String CONTENT = "jcr:content";
	public static final String UUID = "jcr:uuid";
	public static final String LAST_MODIFIED = "jcr:lastModified";
	public static final String LAST_MODIFIED_BY = "jcr:lastModifiedBy";

	/*
	 * Sub Properties of Signup node
	 * 
	 * Example Node: /[JcrName.SIGNUP]/[userName]
	 */
	public static final String USER = "user";
	public static final String PWD = "pwd";
	public static final String EMAIL = "email";
	public static final String CODE = "code";

	public static final String BIN_VER = "binVer";

	/*
	 * I want to use jcr namespace for these since they exist and are known
	 * 
	 * I stopped using jcr:data, when I read docs online saying JCR does try to index content in
	 * binaries, and until I can be sure it's not trying to index images I will just turn off
	 * jcr:data, and use a proprietary property not recognized by jcr
	 */
	public static final String BIN_DATA = "jcrData";
	public static final String BIN_MIME = "jcr:mimeType";

	public static final String IMG_WIDTH = "imgWidth";
	public static final String IMG_HEIGHT = "imgHeight";
}
