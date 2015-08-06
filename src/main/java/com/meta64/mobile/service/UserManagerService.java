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
import org.springframework.ui.Model;

import com.meta64.mobile.config.ConstantsProvider;
import com.meta64.mobile.config.JcrName;
import com.meta64.mobile.config.JcrProp;
import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.mail.JcrOutboxMgr;
import com.meta64.mobile.model.RefInfo;
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
import com.meta64.mobile.util.Encryptor;
import com.meta64.mobile.util.JcrRunnable;
import com.meta64.mobile.util.JcrUtil;
import com.meta64.mobile.util.XString;

/**
 * Service methods for processing user management functions. Login, logout, etc.
 * 
 */
@Component
@Scope("singleton")
public class UserManagerService {
	private static final Logger log = LoggerFactory.getLogger(UserManagerService.class);

	@Value("${anonUserLandingPageNode}")
	private String anonUserLandingPageNode;

	/*
	 * We only use mailHost in this class to detect if email is configured and if not we fail all
	 * signups. Currently this system does require email to be in the process for signing up users.
	 */
	@Value("${mail.host}")
	public String mailHost;

	@Autowired
	private OakRepositoryBean oak;

	@Autowired
	private SessionContext sessionContext;

	@Autowired
	private JcrOutboxMgr outboxMgr;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	@Autowired
	private NodeSearchService nodeSearchService;

	@Autowired
	private ConstantsProvider constProvider;
	
	@Autowired
	private Encryptor encryptor;

	/*
	 * Logs in the user using credentials held in 'req'
	 */
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
			RefInfo rootRefInfo = UserManagerUtil.getRootNodeRefInfoForUser(session, userName);
			sessionContext.setRootRefInfo(rootRefInfo);
			res.setRootNode(rootRefInfo);
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

	/*
	 * Processes last set of signup. Validation of registration code. This means user has clicked
	 * the link they were sent during the signup email verification, and they are sending in a
	 * signupCode that will turn on their account and actually create their account.
	 */
	public void processSignupCode(final String signupCode, final Model model) throws Exception {
		log.debug("User is trying signupCode: "+signupCode);
		adminRunner.run(new JcrRunnable() {
			@Override
			public void run(Session session) throws Exception {
				try {
					Node node = nodeSearchService.findNodeByProperty(session, "/" + JcrName.SIGNUP, //
							JcrProp.CODE, signupCode);
					if (node != null) {
						String userName = JcrUtil.getRequiredStringProp(node, JcrProp.USER);
						String password = JcrUtil.getRequiredStringProp(node, JcrProp.PWD);
						password = encryptor.decrypt(password);
						String email = JcrUtil.getRequiredStringProp(node, JcrProp.EMAIL);

						if (UserManagerUtil.createUser(session, userName, password)) {
							UserManagerUtil.createUserRootNode(session, userName);

							Node prefsNode = getPrefsNodeForSessionUser(session, userName);
							prefsNode.setProperty(JcrProp.EMAIL, email);
							setDefaultUserPreferences(prefsNode);

							/*
							 * allow JavaScript to detect all it needs to detect which is to display
							 * a message to user saying the signup is complete.
							 */
							model.addAttribute("signupCode", "ok");

							log.debug("Removing signup node.");
							node.remove();
							session.save();
							log.debug("Successful signup complete.");
						}
					}
					else {
						throw new Exception("Signup Code is invalid.");
					}
				}
				catch (Exception e) {
					// need to message back to user signup failed.
				}
			}
		});
	}

	/*
	 * Processes a signup request from a user. The user doesn't immediately get an account, but an
	 * email goes out to them that when they click on the link in the email the signupCode comes
	 * back and actually creates their account at that time.
	 */
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
		XString.checkEmail(email);

		/* test cases will simply pass null, for captcha, and we let that pass */
		if (captcha != null && !captcha.equals(sessionContext.getCaptcha())) {
			log.debug("Captcha match!");
			throw new Exception("Wrong captcha text.");
		}

		initiateSignup(userName, password, email);

		res.setMessage("success: " + String.valueOf(++sessionContext.counter));
		res.setSuccess(true);
	}

	/*
	 * Adds user to the JCR list of pending accounts and they will stay in pending status until
	 * their signupCode has been used to validate their email address
	 */
	public void initiateSignup(String userName, String password, String email) throws Exception {

		// TODO: remove hard coded domain from here and put in properties file
		String signupCode = JcrUtil.getGUID();
		String signupLink = constProvider.getHostAndPort() + "?signupCode=" + signupCode;
		String content = "Confirmation for new meta64 account: " + userName + //
				"<p>\nGo to this page to complete signup: <br>\n" + signupLink;

		addPendingSignupNode(userName, password, email, signupCode);

		if (!XString.isEmpty(mailHost)) {
			outboxMgr.queueEmail(email, "Meta64 Account Signup Confirmation", content);
		}
	}

	/*
	 * Creates the node on the tree that holds the user info pending email validation.
	 */
	public void addPendingSignupNode(final String userName, final String password, final String email, final String signupCode) throws Exception {

		adminRunner.run(new JcrRunnable() {
			@Override
			public void run(Session session) throws Exception {

				try {
					Node checkNode = session.getNode("/" + JcrName.SIGNUP + "/" + userName);
					throw new Exception("User name is already pending signup.");
				}
				catch (Exception e) {
					// normal flow. Not an error here.
				}

				Node signupNode = session.getNode("/" + JcrName.SIGNUP);
				if (signupNode == null) {
					throw new Exception("Signup node not found.");
				}

				Node newNode = signupNode.addNode(userName, JcrConstants.NT_UNSTRUCTURED);
				newNode.setProperty(JcrProp.USER, userName);
				newNode.setProperty(JcrProp.PWD, encryptor.encrypt(password));
				newNode.setProperty(JcrProp.EMAIL, email);
				newNode.setProperty(JcrProp.CODE, signupCode);
				JcrUtil.timestampNewNode(session, newNode);
				session.save();
			}
		});
	}

	/*
	 * Get node that contains all preferences for this user, as properties on it.
	 */
	public static Node getPrefsNodeForSessionUser(Session session, String userName) throws Exception {
		return JcrUtil.ensureNodeExists(session, "/userPreferences/", userName, //
				"User: " + userName);
	}

	public void setDefaultUserPreferences(Node prefsNode) throws Exception {
		prefsNode.setProperty(JcrProp.USER_PREF_ADV_MODE, false);
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
				prefsNode.setProperty(JcrProp.USER_PREF_ADV_MODE, req.getUserPreferences().isAdvancedMode());
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
		
				userPrefs.setAdvancedMode(JcrUtil.safeGetBooleanProp(prefsNode, JcrProp.USER_PREF_ADV_MODE));	
				userPrefs.setLastNode(JcrUtil.safeGetStringProp(prefsNode, JcrProp.USER_PREF_LAST_NODE));
			}
		});

		return userPrefs;
	}

	public Node getUserPrefsNode(Session session) throws Exception {

		String userName = sessionContext.getUserName();
		Node allUsersRoot = JcrUtil.getNodeByPath(session, "/" + JcrName.ROOT);
		if (allUsersRoot == null) {
			throw new Exception("/root not found!");
		}

		log.debug("Creating root node, which didn't exist.");

		Node newNode = allUsersRoot.addNode(userName, JcrConstants.NT_UNSTRUCTURED);
		JcrUtil.timestampNewNode(session, newNode);
		if (newNode == null) {
			throw new Exception("unable to create root");
		}

		if (AccessControlUtil.grantFullAccess(session, newNode, userName)) {
			newNode.setProperty(JcrProp.CONTENT, "Root for User: " + userName);
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

		sessionContext.setPassword(req.getNewPassword());
		res.setSuccess(true);
	}
}
