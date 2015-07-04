package com.meta64.mobile.user;

import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.util.JcrRunnable;
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
}
