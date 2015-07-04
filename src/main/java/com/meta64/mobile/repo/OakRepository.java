package com.meta64.mobile.repo;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.sql.DataSource;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.document.DocumentMK;
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore;
import org.apache.jackrabbit.oak.plugins.document.rdb.RDBDataSourceFactory;
import org.apache.jackrabbit.oak.plugins.document.rdb.RDBOptions;
import org.apache.jackrabbit.oak.query.QueryEngineSettings;
import org.apache.jackrabbit.oak.security.SecurityProviderImpl;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.util.JcrRunnable;
import com.meta64.mobile.util.JcrUtil;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoTimeoutException;

/**
 * Wrapper and abstraction around a JCR Repository instance
 *  
 * This class can initialize either a mongoDb or an RDB backing store for the repository. One could argue that 
 * mongo stuff should not be mixed with rdb stuff, but this will be refactored later if it becomes ugly.
 */
public class OakRepository {

	private static final Logger log = LoggerFactory.getLogger(OakRepository.class);

	// TODO: move this string to properties file.
	public static final String TABLEPREFIX = "dstest_";

	private RDBOptions options;
	private DataSource dataSource;
	private DocumentNodeStore nodeStore;
	private Repository repository;
	protected ConfigurationParameters securityParams;
	private SecurityProvider securityProvider;

	private boolean initialized = false;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	public OakRepository() {
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

			repository = new Jcr(new Oak(ns)).with(getQueryEngineSettings()).with(getSecurityProvider()).createRepository();
			log.debug("MongoDb connection ok.");
		}
		catch (MongoTimeoutException e) {
			log.error("********** Did you forget to start MongoDb Server? **********");
			e.printStackTrace();
			throw e;
		}
	}

	public void rdbInit(String url, String user, String pwd) throws Exception {
		if (initialized) {
			throw new Exception("Repository already initialized");
		}
		initialized = true;

		if (options != null || dataSource != null || nodeStore != null || repository != null) {
			throw new Exception("Attempted to initialize repository that is already initialized");
		}

		options = new RDBOptions().tablePrefix(TABLEPREFIX);
		// options = options.dropTablesOnClose(true);

		dataSource = RDBDataSourceFactory.forJdbcUrl(url, user, pwd);

		DocumentMK.Builder builder = new DocumentMK.Builder().setClusterId(1)//
				.memoryCacheSize(64 * 1024 * 1024)//
				.setPersistentCache("target/persistentCache,time")//
				.setRDBConnection(dataSource, options);

		nodeStore = builder.getNodeStore();
		repository = new Jcr(nodeStore).with(getQueryEngineSettings()).with(getSecurityProvider()).createRepository();
	}

	private SecurityProvider getSecurityProvider() {
		Map<String, Object> userParams = new HashMap();
		userParams.put(UserConstants.PARAM_ADMIN_ID, "admin");
		userParams.put(UserConstants.PARAM_OMIT_ADMIN_PW, false);
		userParams.put(UserConstants.NT_REP_PASSWORD, "admin");

		securityParams = ConfigurationParameters.of(UserConfiguration.NAME, ConfigurationParameters.of(userParams));
		securityProvider = new SecurityProviderImpl(securityParams);
		return securityProvider;
	}

	private QueryEngineSettings getQueryEngineSettings() {
		QueryEngineSettings qs = new QueryEngineSettings();
		qs.setFullTextComparisonWithoutIndex(true);
		return qs;
	}

	public void initRequiredNodes() throws Exception {

		adminRunner.run(new JcrRunnable() {
			@Override
			public void run(Session session) throws Exception {

				Node jcrRoot = session.getNode("/");
				if (jcrRoot == null) {
					throw new Exception("System failure. No root");
				}

				Node allUsersRoot = JcrUtil.getNodeByPath(session, "/jcr:root");

				if (allUsersRoot == null) {
					log.debug("Creating jcr:root node, which didn't exist.");

					allUsersRoot = jcrRoot.addNode("jcr:root", JcrConstants.NT_UNSTRUCTURED);
					if (allUsersRoot == null) {
						throw new Exception("unable to create jcr:root");
					}
					allUsersRoot.setProperty("jcr:content", "");
					session.save();
				}
				// log.debug(UserManagerUtil.dumpPrivileges(session, allUsersRoot));
			}
		});
	}

	public void close() {
		log.debug("Closing repository.");
		if (nodeStore != null) {
			nodeStore.dispose();
			nodeStore = null;
		}
		repository = null;
		dataSource = null;
		options = null;
	}
}
