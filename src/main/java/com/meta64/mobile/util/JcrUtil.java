package com.meta64.mobile.util;

import java.util.HashSet;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assorted general utility functions related to JCR nodes.
 */
public class JcrUtil {

	private static final Logger log = LoggerFactory.getLogger(JcrUtil.class);

	/*
	 * These are properties we should never allow the client to send back as part of a save
	 * operation.
	 */
	private static HashSet<String> nonSavableProperties = new HashSet<String>();
	static {
		nonSavableProperties.add("jcr:mixinTypes");
		nonSavableProperties.add("jcr:uuid");

		nonSavableProperties.add("jcr:created");
		nonSavableProperties.add("jcr:createdBy");
		nonSavableProperties.add("jcr:lastModified");
		nonSavableProperties.add("jcr:lastModifiedBy");
	}

	public static Node findNode(Session session, String id) throws Exception {
		return id.startsWith("/") ? session.getNode(id) : session.getNodeByIdentifier(id);
	}

	/*
	 * Currently there's a bug in the client code where it sends nulls for some nonsavable types, so
	 * before even fixing the client I decided to just make the server side block those. This is
	 * more secure to always have the server allow misbehaving javascript for security reasons.
	 */
	public static boolean isSavableProperty(String propertyName) {
		return !nonSavableProperties.contains(propertyName);
	}

	public static void timestampNewNode(Session session, Node node) throws Exception {

		// mix:created -> jcr:created + jcr:createdBy
		if (!node.hasProperty("jcr:created")) {
			node.addMixin("mix:created");
		}

		// mix:lastModified -> jcr:lastModified + jcr:lastModifiedBy
		if (!node.hasProperty("jcr:lastModified")) {
			node.addMixin("mix:lastModified");
		}
	}

	public static Node ensureNodeExists(Session session, String parentPath, String name, String defaultContent) throws Exception {

		Node parent = session.getNode(parentPath);
		if (parent == null) {
			throw new Exception("Expected parent not found: " + parentPath);
		}

		log.debug("ensuring node exists: parentPath=" + parentPath + " name=" + name);
		Node node = JcrUtil.getNodeByPath(session, parentPath + name);

		if (node == null) {
			log.debug("Creating " + name + " node, which didn't exist.");

			node = parent.addNode(name, JcrConstants.NT_UNSTRUCTURED);
			if (node == null) {
				throw new Exception("unable to create " + name);
			}
			node.setProperty("jcr:content", defaultContent);
			session.save();
		}
		log.debug("node found: " + node.getPath());
		return node;
	}

	public static Node getNodeByPath(Session session, String path) {
		try {
			return session.getNode(path);
		}
		catch (Exception e) {
			// do nothing. Not error condition. Means allUsersRoot is not found, so will still be
			// null.
			return null;
		}
	}

	public static int getPropertyCount(Node node) throws RepositoryException {
		PropertyIterator iter = node.getProperties();
		int count = 0;
		while (iter.hasNext()) {
			Property p = iter.nextProperty();
			count++;
		}
		return count;
	}

	/*
	 * I have decided 64bits of randomness is good enough, instead of 128, thus we are dicing up the
	 * string to use every other character. If you want to modify this method to return a full UUID
	 * that will not cause any problems, other than default node names being the full string, which
	 * is kind of long
	 */
	public static String getGUID() throws Exception {
		String uid = UUID.randomUUID().toString();
		StringBuilder sb = new StringBuilder();
		int len = uid.length();

		/* chop length in half by using every other character */
		for (int i = 0; i < len; i += 2) {
			char c = uid.charAt(i);
			if (c == '-') {
				i--;// account for the fact we jump by tow, and start just after dash.
			}
			else {
				sb.append(c);
			}
		}

		return sb.toString();
		// here's another way to generate a random 64bit number...
		// if (prng == null) {
		// prng = SecureRandom.getInstance("SHA1PRNG");
		// }
		//
		// return String.valueOf(prng.nextLong());
	}
}
