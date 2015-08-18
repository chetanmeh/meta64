package com.meta64.mobile.repo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;

import com.meta64.mobile.AppServer;
import com.meta64.mobile.config.JcrName;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.user.UserManagerUtil;
import com.meta64.mobile.util.JcrRunnable;
import com.meta64.mobile.util.JcrUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.oak.api.AuthInfo;
import org.apache.jackrabbit.oak.run.osgi.OakOSGiRepositoryFactory;
import org.apache.jackrabbit.oak.spi.security.authentication.AuthInfoImpl;
import org.apache.jackrabbit.oak.spi.security.principal.AdminPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import static java.util.Collections.singleton;

/**
 * Instance of a MonboDB-based Repository.
 */
@Component
@Scope("singleton")
public class OakRepository {

	private static final Logger log = LoggerFactory.getLogger(OakRepository.class);

	@Value("${indexingEnabled}")
	private boolean indexingEnabled;
	
	private Repository repository;

	/*
	 * Because of the criticality of this variable, I am not using the Spring getter to get it, but
	 * just using a private static. It's slightly safer and better for the purpose of cleanup in the
	 * shutdown hook which is all it's used for.
	 */
	private static OakRepository instance;

	/*
	 * We only need this lock to protect against startup and/or shutdown concurrency. Remember
	 * during debugging, etc the server process can be shutdown (CTRL-C) even while it's in the
	 * startup phase.
	 */
	private static final Object lock = new Object();

	private boolean initialized = false;

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

	@Value("${jcrHome}")
	private String repoHome;

	@Value("classpath:/repository-config.json")
	private Resource defaultRepoConfig;
	
	private String bundleFilter = "(|" +
				"(Bundle-SymbolicName=org.apache.jackrabbit*)" +
				"(Bundle-SymbolicName=org.apache.sling*)" +
				"(Bundle-SymbolicName=org.apache.felix*)" +
				"(Bundle-SymbolicName=org.apache.aries*)" +
				")";

	@Autowired
	private RunAsJcrAdmin adminRunner;

	public OakRepository() {
		instance = this;

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				/*
				 * I know this tight coupling is going to be upsetting to some developers, but this
				 * is a good design despite that. I don't want a complex PUB/SUB or indirection to
				 * get in the way of this working perfectly and being dead simple!
				 */
				AppServer.setShuttingDown(true);
				instance.close();
			}
		}));
	}

	@PostConstruct
	public void postConstruct() throws Exception {
		initRepository();
	}

	@PreDestroy
	public void preDestroy() {
		close();
	}

	public Repository getRepository() {
		return repository;
	}

	public Session newAdminSession() throws RepositoryException {
		//Admin ID here can be any string and need not match the actual admin userId
		final String adminId = "admin";
		Principal admin = new AdminPrincipal() {
			@Override
			public String getName() {
				return adminId;
			}
		};

		//Following approach allows obtaining Admin session without known admin user credentials
		AuthInfo authInfo = new AuthInfoImpl(adminId, null, singleton(admin));
		Subject subject = new Subject(true, singleton(admin), singleton(authInfo), Collections.emptySet());
		Session adminSession;
		try {
			adminSession = Subject.doAsPrivileged(subject, new PrivilegedExceptionAction<Session>() {
				@Override
				public Session run() throws Exception {
					return repository.login();
				}
			}, null);
		} catch (PrivilegedActionException e) {
			throw new RepositoryException("failed to retrieve admin session.", e);
		}

		return adminSession;
	}

	public void close() {
		synchronized (lock) {
			if (!initialized) {
				return;
			}
			initialized = false;

			log.debug("Shutting down repository.");
			if (repository != null) {
				((JackrabbitRepository) repository).shutdown();
				repository = null;
			}
			log.debug("***** All Persistence Shutdown complete. *****");
		}
	}

	public String getJcrAdminUserName() {
		return jcrAdminUserName;
	}

	public String getJcrAdminPassword() {
		return jcrAdminPassword;
	}

	private void initRepository() throws Exception {
		File repoHomeDir = new File(repoHome);
		FileUtils.forceMkdir(repoHomeDir);

		File repoConfig = new File(repoHomeDir, "repository-config.json");
		copyDefaultConfig(repoConfig, defaultRepoConfig);
		repository = createRepository(repoConfig, repoHomeDir);

		UserManagerUtil.verifyAdminAccountReady(this);
		initRequiredNodes();
		adminRunner.run(new IndexInitializer());
		initialized = true;
	}

	private Repository createRepository(File repoConfig, File repoHomeDir) throws RepositoryException {
		Map<String,Object> config = new HashMap<>();
		config.put(OakOSGiRepositoryFactory.REPOSITORY_HOME, repoHomeDir.getAbsolutePath());
		config.put(OakOSGiRepositoryFactory.REPOSITORY_CONFIG_FILE, repoConfig.getAbsolutePath());
		config.put(OakOSGiRepositoryFactory.REPOSITORY_BUNDLE_FILTER, bundleFilter);
		config.put(OakOSGiRepositoryFactory.REPOSITORY_SHUTDOWN_ON_TIMEOUT, false);
		config.put(OakOSGiRepositoryFactory.REPOSITORY_ENV_SPRING_BOOT, true);
		config.put(OakOSGiRepositoryFactory.REPOSITORY_TIMEOUT_IN_SECS, 10);

		//Pass on config required for substitution in OSGi config
		config.put("mongodb.host", mongoDbHost);
		config.put("mongodb.port", mongoDbPort);
		config.put("mongodb.name", mongoDbName);

		return new OakOSGiRepositoryFactory().getRepository(config);
	}

	private void initRequiredNodes() throws Exception {
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

	private static void copyDefaultConfig(File repoConfig, Resource defaultRepoConfig)
			throws IOException, RepositoryException {
		if (!repoConfig.exists()){
			InputStream in = defaultRepoConfig.getInputStream();
			if (in == null){
				throw new RepositoryException("No config file found in classpath " + defaultRepoConfig);
			}
			OutputStream os = null;
			try {
				os = FileUtils.openOutputStream(repoConfig);
				IOUtils.copy(in, os);
			} finally {
				IOUtils.closeQuietly(os);
				IOUtils.closeQuietly(in);
			}
		}
	}
}
