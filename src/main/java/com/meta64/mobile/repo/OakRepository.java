package com.meta64.mobile.repo;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jcr.Repository;
import javax.jcr.Session;

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

	private DocumentNodeStore nodeStore;
	private DocumentNodeState root;
	private Repository repository;
	protected ConfigurationParameters securityParams;
	private SecurityProvider securityProvider;

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

	public Repository getRepository() {
		return repository;
	}

	public void mongoInit(String mongoDbHost, int mongoDbPort, String mongoDbName) throws Exception {
		if (initialized) {
			throw new Exception("Repository already initialized");
		}
		initialized = true;

		try {
			DB db = new MongoClient(mongoDbHost, mongoDbPort).getDB(mongoDbName);
			DocumentNodeStore ns = new DocumentMK.Builder().setMongoDB(db).getNodeStore();
			root = ns.getRoot();

			/*
			 * This code appears to successfully initialize a new repository, but just be aware when
			 * you change this code you have to blow away your repository and let a new one be
			 * created.
			 * 
			 * WARNING: Not all valid SQL will work with these lucene queries. Namely the contains()
			 * method fails so always use '=' operator for exact string matches or LIKE %something%,
			 * instead of using the contains method.
			 */
			LuceneIndexProvider provider = new LuceneIndexProvider().with(getNodeAggregator());
			repository = new Jcr(new Oak(ns))//
					.with(getSecurityProvider())//
					.with(new LuceneFullTextInitializer("contentIndex", "jcr:content", (Set<String>) null))//
					.with(new LuceneSortInitializer("lastModifiedIndex", "jcr:lastModified", (Set<String>) null))//
					.with(new LuceneSortInitializer("codeIndex", "code", (Set<String>) null)).with((QueryIndexProvider) provider)//
					.with((Observer) provider)//
					.with(new LuceneIndexEditorProvider())//
					.createRepository();

			log.debug("MongoDb connection ok.");
		}
		catch (MongoTimeoutException e) {
			log.error("********** Did you forget to start MongoDb Server? **********");
			e.printStackTrace();
			throw e;
		}
	}

	/* TODO: I don't know what this aggregator is or if I need it */
	private static NodeAggregator getNodeAggregator() {
		return new SimpleNodeAggregator().newRuleWithName(JcrConstants.NT_UNSTRUCTURED, //
				newArrayList(JCR_CONTENT, JCR_CONTENT + "/*"));
	}

	private SecurityProvider getSecurityProvider() {
		Map<String, Object> userParams = new HashMap();
		userParams.put(UserConstants.PARAM_ADMIN_ID, "admin");
		userParams.put(UserConstants.PARAM_OMIT_ADMIN_PW, false);

		securityParams = ConfigurationParameters.of(ImmutableMap.of(UserConfiguration.NAME, ConfigurationParameters.of(userParams)));
		securityProvider = new SecurityProviderImpl(securityParams);
		return securityProvider;
	}

	public void close() {
		log.debug("Closing repository.");
		if (nodeStore != null) {
			nodeStore.dispose();
			nodeStore = null;
		}
		repository = null;
	}

	public DocumentNodeState getRoot() {
		return root;
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
