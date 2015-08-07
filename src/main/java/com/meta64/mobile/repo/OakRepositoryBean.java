package com.meta64.mobile.repo;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jcr.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.JcrName;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.user.UserManagerUtil;
import com.meta64.mobile.util.JcrRunnable;
import com.meta64.mobile.util.JcrUtil;

/**
 * Instance of a Repository.
 * 
 * TODO: I think I no longer need the base class as a separate class, any longer. I originally had a
 * good reason for this but due to other refactorings, now I see that this base class is no longer
 * needed. Just use one class.
 */
@Component
@Scope("singleton")
public class OakRepositoryBean extends OakRepository {

	/*
	 * MongoDb Server Connection Info
	 */
	@Value("${mongodb.host}")
	private String mongoDbHost;

	@Value("${mongodb.port}")
	private Integer mongoDbPort;

	@Value("${mongodb.name}")
	private String mongoDbName;

	/*
	 * JCR Info
	 */
	@Value("${jcrAdminUserName}")
	private String jcrAdminUserName;

	@Value("${jcrAdminPassword}")
	private String jcrAdminPassword;

	@Value("${anonUserLandingPageNode}")
	private String userLandingPageNode;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	@PostConstruct
	public void postConstruct() throws Exception {

		mongoInit(mongoDbHost, mongoDbPort, mongoDbName);

		UserManagerUtil.verifyAdminAccountReady(this);
		initRequiredNodes();
	}

	public void initRequiredNodes() throws Exception {

		adminRunner.run(new JcrRunnable() {
			@Override
			public void run(Session session) throws Exception {
				JcrUtil.ensureNodeExists(session, "/", userLandingPageNode, null);
				JcrUtil.ensureNodeExists(session, "/", JcrName.ROOT, "Root of All Users");
				JcrUtil.ensureNodeExists(session, "/", JcrName.USER_PREFERENCES, "Preferences of All Users");
				JcrUtil.ensureNodeExists(session, "/", JcrName.OUTBOX, "System Email Outbox");
				JcrUtil.ensureNodeExists(session, "/", JcrName.SIGNUP, "Pending Signups");
			}
		});
	}

	@PreDestroy
	public void preDestroy() {
		close();
	}

	public String getJcrAdminUserName() {
		return jcrAdminUserName;
	}

	public String getJcrAdminPassword() {
		return jcrAdminPassword;
	}
}
