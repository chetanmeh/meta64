package com.meta64.mobile.repo;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Repository;

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

import com.google.common.collect.ImmutableMap;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoTimeoutException;

/**
 * Wrapper and abstraction around a JCR Repository instance
 */
public class OakRepository {

	private static final Logger log = LoggerFactory.getLogger(OakRepository.class);

	private DocumentNodeStore nodeStore;
	private DocumentNodeState root;
	private Repository repository;
	protected ConfigurationParameters securityParams;
	private SecurityProvider securityProvider;

	private boolean initialized = false;

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
			root = ns.getRoot();

			/*
			 * This code appears to successfully initialize a new repository, but just be aware when
			 * you change this code you have to blow away your repository and let a new one be
			 * created.
			 */
			LuceneIndexProvider provider = new LuceneIndexProvider().with(getNodeAggregator());
			repository = new Jcr(new Oak(ns))//
					.with(getSecurityProvider())//
					.with(new LuceneFullTextInitializer("contentIndex", "jcr:content", (Set<String>) null))//
					.with(new LuceneSortInitializer("lastModifiedIndex", "jcr:lastModified", (Set<String>) null))//
					.with((QueryIndexProvider) provider)//
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
}
