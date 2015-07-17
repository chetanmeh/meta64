package com.meta64.mobile.user;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.model.UserPreferences;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.request.SaveUserPreferencesRequest;
import com.meta64.mobile.response.SaveUserPreferencesResponse;
import com.meta64.mobile.util.JcrRunnable;
import com.meta64.mobile.util.JcrUtil;
import com.meta64.mobile.util.ValContainer;
import com.meta64.mobile.util.XString;

/**
 * Service methods for processing user management functions. Login, logout, etc.
 * 
 */
@Component
@Scope("session")
public class UserManagerService {
	private static final Logger log = LoggerFactory.getLogger(UserManagerService.class);

	@Autowired
	private OakRepositoryBean oak;

	@Autowired
	private SessionContext sessionContext;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	/* returns true if successful */
	public boolean signup(final String userName, final String password, String email, String captcha) throws Exception {
		log.debug("Signup: userName=" + userName + " email=" + email + " captcha=" + captcha);

		/* throw exceptions of the username or password are not valid */
		XString.checkUserName(userName);
		XString.checkPassword(password);

		final ValContainer<Boolean> success = new ValContainer<Boolean>(false);

		/* test cases will simply pass null, for captcha, and we let that pass */
		if (captcha != null && !captcha.equals(sessionContext.getCaptcha())) {
			log.debug("Captcha match!");
			throw new Exception("Wrong captcha text.");
		}

		adminRunner.run(new JcrRunnable() {
			@Override
			public void run(Session session) throws Exception {
				if (UserManagerUtil.createUser(session, userName, password)) {
					UserManagerUtil.createUserRootNode(session, userName);
					success.setVal(true);
				}
			}
		});

		return success.getVal();
	}

	/*
	 * Get node that contains all preferences for this user, as properties on it.
	 */
	public Node getPrefsNodeForSessionUser(Session session) throws Exception {
		return oak.ensureNodeExists(session, "/jcr:userPreferences/", "jcr:" + sessionContext.getUserName(), //
				"Preferences fo User: " + sessionContext.getUserName());
	}

	public void saveUserPreferences(Session session, SaveUserPreferencesRequest req, SaveUserPreferencesResponse res) throws Exception {
		Node prefsNode = getPrefsNodeForSessionUser(session);

		/*
		 * Assign preferences as properties on this node,
		 */
		prefsNode.setProperty("jcr:advMode", req.getUserPreferences().isAdvancedMode());

		// We let session.save() be done by calling method.
	}

	public UserPreferences getUserPreferences(Session session) throws Exception {
		Node prefsNode = getPrefsNodeForSessionUser(session);
		UserPreferences userPrefs = new UserPreferences();

		Property prop = prefsNode.getProperty("jcr:advMode");
		userPrefs.setAdvancedMode(prop != null ? prop.getBoolean() : false);
		log.debug("advMode="+userPrefs.isAdvancedMode());

		return userPrefs;
	}

	public Node getUserPrefsNode(Session session) throws Exception {

		String userName = sessionContext.getUserName();
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

		return allUsersRoot;
	}

}
