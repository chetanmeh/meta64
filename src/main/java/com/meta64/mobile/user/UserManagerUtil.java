package com.meta64.mobile.user;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meta64.mobile.model.RefInfo;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.util.JcrUtil;

/**
 * Utilities related to user management.
 *
 */
public class UserManagerUtil {
	private static final Logger log = LoggerFactory.getLogger(UserManagerUtil.class);

	public static boolean createUser(Session session, String userName, String password) throws Exception {
		boolean ret = false;
		UserManager userManager = ((JackrabbitSession) session).getUserManager();
		Authorizable authorizable = userManager.getAuthorizable(userName);
		if (authorizable == null) {
			User user = userManager.createUser(userName, password);
			if (user != null) {
				session.save();
				ret = true;
			}
		}
		else {
			throw new Exception("UserName is already taken.");
		}
		return ret;
	}

	public static RefInfo getRootNodeRefInfoForUser(Session session, String userName) throws Exception {
		Node rootNode = null;
		if (userName.equalsIgnoreCase("admin")) {
			rootNode = session.getRootNode();
		}
		else {
			rootNode = session.getNode("/jcr:root/jcr:" + userName);
		}
		return new RefInfo(rootNode.getIdentifier(), rootNode.getPath());
	}

	/*
	 * TODO: refactor this method to use ensureNodeExists do look up existing node(s), Also throw
	 * exception if the node we are looking up EXISTS, because someone could be hijacking an old
	 * account.
	 */
	public static boolean createUserRootNode(Session session, String userName) throws Exception {

		Node allUsersRoot = JcrUtil.getNodeByPath(session, "/jcr:root");
		if (allUsersRoot == null) {
			throw new Exception("/jcr:root not found!");
		}

		log.debug("Creating jcr:root node, which didn't exist.");

		Node newNode = allUsersRoot.addNode("jcr:" + userName, JcrConstants.NT_UNSTRUCTURED);
		JcrUtil.timestampNewNode(session, newNode);
		if (newNode == null) {
			throw new Exception("unable to create jcr:root");
		}

		if (AccessControlUtil.grantFullAccess(session, newNode, userName)) {
			newNode.setProperty("jcr:content", "Root for User: " + userName);
			session.save();
		}

		return true;
	}

	public static void changePassword(Session session, String newPassword) throws Exception {
		UserManager userManager = ((JackrabbitSession) session).getUserManager();
		String userId = session.getUserID();
		Authorizable authorizable = userManager.getAuthorizable(userId);
		((User) authorizable).changePassword(newPassword);
	}

	public static void verifyAdminAccountReady(OakRepositoryBean oak) throws Exception {

		Session session = null;

		try {
			session = oak.getRepository().login(new SimpleCredentials(oak.getJcrAdminUserName(), oak.getJcrAdminPassword().toCharArray()));
			log.debug("Admin user login verified, on first attempt.");
		}
		catch (Exception e) {
			log.debug("Admin account credentials not working. Trying with default admin/admin.");

			try {
				session = oak.getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));
				log.debug("Admin user login verified, using defaults.");

				changePassword(session, oak.getJcrAdminPassword());
				session.save();
			}
			catch (Exception e2) {
				log.debug("Admin user login failed with configured credentials AND default. Unable to connect. Server will fail.");
				throw e2;
			}
		}
		finally {
			if (session != null) {
				session.logout();
				session = null;
			}
		}
	}
}
