package com.meta64.mobile.service;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.model.UserPreferences;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.request.ChangePasswordRequest;
import com.meta64.mobile.request.LoginRequest;
import com.meta64.mobile.request.SaveUserPreferencesRequest;
import com.meta64.mobile.request.SignupRequest;
import com.meta64.mobile.response.ChangePasswordResponse;
import com.meta64.mobile.response.LoginResponse;
import com.meta64.mobile.response.SaveUserPreferencesResponse;
import com.meta64.mobile.response.SignupResponse;
import com.meta64.mobile.user.AccessControlUtil;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.user.UserManagerUtil;
import com.meta64.mobile.util.DateUtil;
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

	@Value("${anonUserLandingPageNode}")
	private String anonUserLandingPageNode;

	@Autowired
	private OakRepositoryBean oak;

	@Autowired
	private SessionContext sessionContext;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	public void login(Session session, LoginRequest req, LoginResponse res) throws Exception {
		String userName = req.getUserName();
		String password = req.getPassword();

		sessionContext.setTimezone(DateUtil.getTimezoneFromOffset(req.getTzOffset()));
		sessionContext.setTimeZoneAbbrev(DateUtil.getUSTimezone(-req.getTzOffset() / 60, req.isDst()));

		if (userName.equals("{session}")) {
			userName = sessionContext.getUserName();
		}
		else {
			sessionContext.setUserName(userName);
			sessionContext.setPassword(password);
		}

		if (session == null) {
			/*
			 * Note: This is not an error condition, this happens whenever the page loads for the
			 * first time and the user has no session yet,
			 */
			res.setUserName("anonymous");
			res.setMessage("not logged in.");
			res.setSuccess(false);
		}
		else {
			res.setRootNode(UserManagerUtil.getRootNodeRefInfoForUser(session, userName));
			res.setUserName(userName);

			try {
				res.setUserPreferences(getUserPreferences(session));
			}
			catch (Exception e) {
				/*
				 * If something goes wrong loading preferences just log and continue. Should never
				 * happen but we might as well be resilient here.
				 */
				// log.error("Failed loading preferences: ", e);
			}
			res.setSuccess(true);
		}
		res.setAnonUserLandingPageNode(anonUserLandingPageNode);
		res.setHomeNodeOverride(sessionContext.getUrlId());

		if (res.getUserPreferences() == null) {
			res.setUserPreferences(getDefaultUserPreferences());
		}
	}

	public void signup(SignupRequest req, SignupResponse res) throws Exception {

		final String userName = req.getUserName();
		if (userName.equalsIgnoreCase("admin") || userName.equalsIgnoreCase("administrator")) {
			throw new Exception("Sorry, you can't be the new admin.");
		}

		if (userName.equalsIgnoreCase("everyone")) {
			throw new Exception("Sorry, you can't be everyone.");
		}

		final String password = req.getPassword();
		final String email = req.getEmail();
		final String captcha = req.getCaptcha() == null ? "" : req.getCaptcha();

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

		if (success.getVal()) {
			res.setMessage("success: " + String.valueOf(++sessionContext.counter));
			res.setSuccess(true);
		}
	}

	/*
	 * Get node that contains all preferences for this user, as properties on it.
	 */
	public Node getPrefsNodeForSessionUser(Session session, String userName) throws Exception {
		return JcrUtil.ensureNodeExists(session, "/userPreferences/", userName, //
				"User: " + userName);
	}

	public void saveUserPreferences(Session session, final SaveUserPreferencesRequest req, final SaveUserPreferencesResponse res) throws Exception {

		final String userName = sessionContext.getUserName();

		adminRunner.run(new JcrRunnable() {
			@Override
			public void run(Session session) throws Exception {
				Node prefsNode = getPrefsNodeForSessionUser(session, userName);

				/*
				 * Assign preferences as properties on this node,
				 */
				prefsNode.setProperty("advMode", req.getUserPreferences().isAdvancedMode());
				session.save();
			}
		});
	}

	public UserPreferences getDefaultUserPreferences() {
		return new UserPreferences();
	}

	public UserPreferences getUserPreferences(Session session) throws Exception {
		final String userName = sessionContext.getUserName();
		final UserPreferences userPrefs = new UserPreferences();

		adminRunner.run(new JcrRunnable() {
			@Override
			public void run(Session session) throws Exception {
				Node prefsNode = getPrefsNodeForSessionUser(session, userName);
				Property prop = prefsNode.getProperty("advMode");
				userPrefs.setAdvancedMode(prop != null ? prop.getBoolean() : false);
				log.debug("advMode=" + userPrefs.isAdvancedMode());
			}
		});

		return userPrefs;
	}

	public Node getUserPrefsNode(Session session) throws Exception {

		String userName = sessionContext.getUserName();
		Node allUsersRoot = JcrUtil.getNodeByPath(session, "/root");
		if (allUsersRoot == null) {
			throw new Exception("/jcr:root not found!");
		}

		log.debug("Creating root node, which didn't exist.");

		Node newNode = allUsersRoot.addNode(userName, JcrConstants.NT_UNSTRUCTURED);
		JcrUtil.timestampNewNode(session, newNode);
		if (newNode == null) {
			throw new Exception("unable to create root");
		}

		if (AccessControlUtil.grantFullAccess(session, newNode, userName)) {
			newNode.setProperty("jcr:content", "Root for User: " + userName);
			session.save();
		}

		return allUsersRoot;
	}

	public void changePassword(Session session, final ChangePasswordRequest req, ChangePasswordResponse res) throws Exception {
		final String userName = sessionContext.getUserName();

		adminRunner.run(new JcrRunnable() {
			@Override
			public void run(Session session) throws Exception {
				UserManagerUtil.changePassword(session, userName, req.getNewPassword());
				session.save();
			}
		});

		// UserManagerUtil.changePassword(session, req.getNewPassword());
		// session.save();
		sessionContext.setPassword(req.getNewPassword());
		res.setSuccess(true);
	}
}
