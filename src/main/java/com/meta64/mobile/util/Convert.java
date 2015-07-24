package com.meta64.mobile.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.Privilege;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.meta64.mobile.config.AppConstant;
import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.image.ImageSize;
import com.meta64.mobile.image.ImageUtil;
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

	public static final PropertyInfoComparator propertyInfoComparator = new PropertyInfoComparator();

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

		if (aclEntries != null) {
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
		}

		return aclEntriesInfo;
	}

	/* WARNING: skips the check for ordered children and just assigns false for performance reasons */
	public static NodeInfo convertToNodeInfo(SessionContext sessionContext, Session session, Node node) throws Exception {
		boolean hasBinary = false;
		boolean binaryIsImage = false;
		long binVer = 0;
		ImageSize imageSize = null;
		try {
			// TODO: is there some better performing way of checking for the existence of a node ?
			// Node binNode = session.getNode(node.getPath() + "/" + AppConstant.JCR_PROP_BIN);

			binVer = getBinaryVersion(node);
			if (binVer > 0) {

				/* if we didn't get an exception, we know we have a binary */
				hasBinary = true;
				binaryIsImage = isImageAttached(node);

				if (binaryIsImage) {
					imageSize = getImageSize(node);
				}
			}
		}
		catch (Exception e) {
			// not an error. means node has no binary subnode.
		}

		/*
		 * node.hasNodes() won't work here, because the gui doesn't display nt:bin nodes as actual
		 * nodes
		 */
		boolean hasDisplayableNodes = node.hasNodes(); // hasDisplayableNodes(node);

		NodeInfo nodeInfo = new NodeInfo(node.getIdentifier(), node.getPath(), node.getName(), buildPropertyInfoList(sessionContext, node), hasDisplayableNodes, false,
				hasBinary, binaryIsImage, binVer, //
				imageSize != null ? imageSize.getWidth() : 0, //
				imageSize != null ? imageSize.getHeight() : 0);
		return nodeInfo;
	}

	/*
	 * Repository doesn't show binaries as actual nodes, so we need to find out using this method if
	 * there are any non-binary nodes, so this returns true if there are some nodes that aren't
	 * binaries.
	 */
	// public static boolean hasDisplayableNodes(Node node) throws Exception {
	// NodeIterator nodeIter = node.getNodes();
	// try {
	// while (true) {
	// Node n = nodeIter.nextNode();
	// if (!n.getName().equals(AppConstant.JCR_PROP_BIN)) {
	// return true;
	// }
	// }
	// }
	// catch (NoSuchElementException ex) {
	// // not an error. Normal iterator end condition.
	// }
	// return false;
	// }

	public static long getBinaryVersion(Node node) throws Exception {
		Property versionProperty = node.getProperty(AppConstant.JCR_PROP_BIN_VER);
		if (versionProperty != null) {
			return versionProperty.getValue().getLong();
		}
		return 0;
	}

	public static ImageSize getImageSize(Node node) throws Exception {
		ImageSize imageSize = new ImageSize();

		Property widthProperty = node.getProperty(AppConstant.JCR_PROP_IMG_WIDTH);
		if (widthProperty != null) {
			imageSize.setWidth((int) widthProperty.getValue().getLong());
		}

		Property heightProperty = node.getProperty(AppConstant.JCR_PROP_IMG_HEIGHT);
		if (heightProperty != null) {
			imageSize.setHeight((int) heightProperty.getValue().getLong());
		}
		return imageSize;
	}

	public static boolean isImageAttached(Node node) throws Exception {
		Property mimeTypeProp = node.getProperty(AppConstant.JCR_PROP_BIN_MIME);
		return (mimeTypeProp != null && //
				mimeTypeProp.getValue() != null && //
		ImageUtil.isImageMime(mimeTypeProp.getValue().getString()));
	}

	public static List<PropertyInfo> buildPropertyInfoList(SessionContext sessionContext, Node node) throws RepositoryException {
		List<PropertyInfo> props = null;
		PropertyIterator iter = node.getProperties();
		PropertyInfo contentPropInfo = null;

		while (iter.hasNext()) {
			/* lazy create props */
			if (props == null) {
				props = new LinkedList<PropertyInfo>();
			}
			Property p = iter.nextProperty();
			PropertyInfo propInfo = convertToPropertyInfo(sessionContext, p);
			// if (Log.renderNodeRequest) {
			// log.debug("   PROP Name: " + p.getName());
			// }

			/*
			 * grab the content property, and don't put it in the return list YET, because we will
			 * be sorting the list and THEN putting the content at the top of that sorted list.
			 */
			if (p.getName().equals("jcr:content")) {
				contentPropInfo = propInfo;
			}
			else {
				props.add(propInfo);
			}
		}
		Collections.sort(props, propertyInfoComparator);

		/* put content prop always at top of list */
		if (contentPropInfo != null) {
			props.add(0, contentPropInfo);
		}
		return props;
	}

	public static PropertyInfo convertToPropertyInfo(SessionContext sessionContext, Property prop) throws RepositoryException {
		String value = null;
		List<String> values = null;

		/* multivalue */
		if (prop.isMultiple()) {
			values = new LinkedList<String>();
			for (Value v : prop.getValues()) {
				values.add(formatValue(sessionContext, v));
			}
		}
		/* else single value */
		else {
			value = formatValue(sessionContext, prop.getValue());
		}
		PropertyInfo propInfo = new PropertyInfo(prop.getType(), prop.getName(), value, values);
		return propInfo;
	}

	public static String formatValue(SessionContext sessionContext, Value value) {
		try {
			if (value.getType() == PropertyType.DATE) {
				return sessionContext.formatTime(value.getDate().getTime());
			}
			else {
				return value.getString();
			}
		}
		catch (Exception e) {
			return "[date??]";
		}
	}
}
