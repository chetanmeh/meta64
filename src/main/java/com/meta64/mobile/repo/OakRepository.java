package com.meta64.mobile.repo;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.document.DocumentMK;
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeState;
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore;
import org.apache.jackrabbit.oak.plugins.index.aggregate.NodeAggregator;
import org.apache.jackrabbit.oak.plugins.index.aggregate.SimpleNodeAggregator;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexProvider;
import org.apache.jackrabbit.oak.security.SecurityProviderImpl;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.jackrabbit.oak.spi.query.QueryIndexProvider;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.meta64.mobile.AppServer;
import com.meta64.mobile.config.JcrName;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.user.UserManagerUtil;
import com.meta64.mobile.util.JcrRunnable;
import com.meta64.mobile.util.JcrUtil;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoTimeoutException;

/**
 * Instance of a MonboDB-based Repository.
 */
@Component
@Scope("singleton")
public class OakRepository {

	private static final Logger log = LoggerFactory.getLogger(OakRepository.class);

	private boolean indexEnabled = true;
	private LuceneIndexProvider indexProvider;
	private DocumentNodeStore nodeStore;
	private DocumentNodeState root;
	private Repository repository;
	protected ConfigurationParameters securityParams;
	private SecurityProvider securityProvider;

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
		mongoInit(mongoDbHost, mongoDbPort, mongoDbName);
	}

	@PreDestroy
	public void preDestroy() {
		close();
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

	public Repository getRepository() {
		return repository;
	}

	public Session newAdminSession() throws Exception {
		return repository.login(new SimpleCredentials(getJcrAdminUserName(), getJcrAdminPassword().toCharArray()));
	}

	public void mongoInit(String mongoDbHost, int mongoDbPort, String mongoDbName) throws Exception {
		synchronized (lock) {
			if (initialized) {
				throw new Exception("Repository already initialized");
			}
			initialized = true;

			try {
				DB db = new MongoClient(mongoDbHost, mongoDbPort).getDB(mongoDbName);
				DocumentNodeStore ns = new DocumentMK.Builder().setMongoDB(db).getNodeStore();
				root = ns.getRoot();

				/* can shutdown during startup. */
				if (AppServer.isShuttingDown()) return;

				Jcr jcr = new Jcr(new Oak(ns));
				jcr = jcr.with(getSecurityProvider());

				if (indexEnabled) {
					/*
					 * WARNING: Not all valid SQL will work with these lucene queries. Namely the
					 * contains() method fails so always use '=' operator for exact string matches
					 * or LIKE %something%, instead of using the contains method.
					 */
					indexProvider = new LuceneIndexProvider();
					indexProvider = indexProvider.with(getNodeAggregator());
					jcr = jcr.with(new LuceneFullTextInitializer("contentIndex", "jcr:content"));
					jcr = jcr.with(new LuceneSortInitializer("lastModifiedIndex", "jcr:lastModified"));
					jcr = jcr.with(new LuceneSortInitializer("codeIndex", "code"));
					jcr = jcr.with((QueryIndexProvider) indexProvider);
					jcr = jcr.with((Observer) indexProvider);
					jcr = jcr.with(new LuceneIndexEditorProvider());
				}

				/* can shutdown during startup. */
				if (AppServer.isShuttingDown()) return;

				repository = jcr.createRepository();

				log.debug("MongoDb connection ok.");

				/* can shutdown during startup. */
				if (AppServer.isShuttingDown()) return;

				UserManagerUtil.verifyAdminAccountReady(this);
				initRequiredNodes();

				log.debug("Repository fully initialized.");
			}
			catch (MongoTimeoutException e) {
				log.error("********** Did you forget to start MongoDb Server? **********", e);
				throw e;
			}
		}
	}

	/* TODO: I don't know what this aggregator is or if I need it */
	private static NodeAggregator getNodeAggregator() {
		return new SimpleNodeAggregator().newRuleWithName(JcrConstants.NT_UNSTRUCTURED, //
				newArrayList(JCR_CONTENT, JCR_CONTENT + "/*"));
	}

	private SecurityProvider getSecurityProvider() {
		Map<String, Object> userParams = new HashMap<String, Object>();
		userParams.put(UserConstants.PARAM_ADMIN_ID, "admin");
		userParams.put(UserConstants.PARAM_OMIT_ADMIN_PW, false);

		securityParams = ConfigurationParameters.of(ImmutableMap.of(UserConfiguration.NAME, ConfigurationParameters.of(userParams)));
		securityProvider = new SecurityProviderImpl(securityParams);
		return securityProvider;
	}

	public void close() {
		synchronized (lock) {
			if (!initialized) {
				return;
			}
			initialized = false;

			log.debug("Closing nodeStore.");
			if (nodeStore != null) {
				nodeStore.dispose();
				nodeStore = null;
			}

			log.debug("Closing indexProvider.");
			if (indexProvider != null) {
				indexProvider.close();
				indexProvider = null;
			}
			repository = null;
			log.debug("Repository close complete.");
		}
	}

	public DocumentNodeState getRoot() {
		return root;
	}

	public String getJcrAdminUserName() {
		return jcrAdminUserName;
	}

	public String getJcrAdminPassword() {
		return jcrAdminPassword;
	}
}
