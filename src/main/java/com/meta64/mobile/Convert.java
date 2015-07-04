package com.meta64.mobile;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.Privilege;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.meta64.mobile.model.AccessControlEntryInfo;
import com.meta64.mobile.model.NodeInfo;
import com.meta64.mobile.model.PrivilegeInfo;
import com.meta64.mobile.model.PropertyInfo;

/**
 * Converting objects from one type to another, and formatting.
 */
public class Convert {

	private static final ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}

	private static final Logger log = LoggerFactory.getLogger(Convert.class);

	public static String JsonStringify(Object obj) throws Exception {
		/*
		 * there ARE performance gains from reusing the same object, but the docs on whether
		 * ObjectMapper is threadsafe appear to be written by someone not well-versed in threads (at
		 * least what I found), so for now, i'm just adding thread safety at this layer, to ensure.
		 * (TODO, remove synchronzie block if truly it's safe)
		 */
		synchronized (mapper) {
			return mapper.writeValueAsString(obj);
		}
	}

	public static List<AccessControlEntryInfo> convertToAclListInfo(AccessControlEntry[] aclEntries) {
		List<AccessControlEntryInfo> aclEntriesInfo = new LinkedList<AccessControlEntryInfo>();

		for (AccessControlEntry ace : aclEntries) {
			AccessControlEntryInfo aceInfo = new AccessControlEntryInfo();
			aceInfo.setPrincipalName(ace.getPrincipal().getName());

			List<PrivilegeInfo> privInfoList = new LinkedList<PrivilegeInfo>();
			for (Privilege privilege : ace.getPrivileges()) {

				PrivilegeInfo privInfo = new PrivilegeInfo();
				privInfo.setPrivilegeName(privilege.getName());
				privInfoList.add(privInfo);
			}

			aceInfo.setPrivileges(privInfoList);
			aclEntriesInfo.add(aceInfo);
		}

		return aclEntriesInfo;
	}

	/* WARNING: skips the check for ordered children and just assigns false for performance reasons */
	public static NodeInfo convertToNodeInfo(Session session, Node node) throws RepositoryException {
		boolean hasBinary = false;
		boolean binaryIsImage = false;
		long binVer = 0;
		try {
			// TODO: is there some better performing way of checking for the existence of a node ?
			Node binNode = session.getNode(node.getPath() + "/nt:bin");

			/* if we didn't get an exception, we know we have a binary */
			hasBinary = true;

			binaryIsImage = isImageAttached(binNode);
			binVer = getBinaryVersion(binNode);
		}
		catch (Exception e) {
			// not an error. means node has no binary subnode.
		}
		NodeInfo nodeInfo = new NodeInfo(node.getIdentifier(), node.getPath(), node.getName(), convertToPropertyInfoList(node), node.hasNodes(), false, hasBinary,
				binaryIsImage, binVer);
		return nodeInfo;
	}

	public static long getBinaryVersion(Node node) throws Exception {
		Property versionProperty = node.getProperty(AppController.NAMESPACE + ":ver");
		if (versionProperty != null) {
			return versionProperty.getValue().getLong();
		}
		return -1;
	}

	public static boolean isImageAttached(Node node) throws Exception {
		Property mimeTypeProp = node.getProperty("jcr:mimeType");
		return (mimeTypeProp != null && //
				mimeTypeProp.getValue() != null && //
				mimeTypeProp.getValue().getString() != null && //
		mimeTypeProp.getValue().getString().toLowerCase().startsWith("image/"));
	}

	public static List<PropertyInfo> convertToPropertyInfoList(Node node) throws RepositoryException {
		List<PropertyInfo> props = null;
		PropertyIterator iter = node.getProperties();

		while (iter.hasNext()) {
			/* lazy create props */
			if (props == null) {
				props = new LinkedList<PropertyInfo>();
			}
			Property p = iter.nextProperty();
			PropertyInfo propInfo = convertToPropertyInfo(p);
			if (Log.renderNodeRequest) {
				log.debug("   PROP Name: " + p.getName());
			}
			props.add(propInfo);
		}
		return props;
	}

	public static PropertyInfo convertToPropertyInfo(Property prop) throws RepositoryException {
		String value = null;
		List<String> values = null;

		/* multivalue */
		if (prop.isMultiple()) {
			values = new LinkedList<String>();
			for (Value v : prop.getValues()) {
				values.add(v.getString());
			}
		}
		/* else single value */
		else {
			value = prop.getString();
		}
		PropertyInfo propInfo = new PropertyInfo(prop.getType(), prop.getName(), value, values);
		return propInfo;
	}
}
