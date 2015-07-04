package com.meta64.mobile.repo;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.user.UserManagerUtil;

/**
 * Instance of a Repository
 */
@Component
@Scope("singleton")
public class OakRepositoryBean extends OakRepository {

	/*
	 * Relational Database Connection Info
	 */
	@Value("${rdb.connection}")
	private String rdbConnection;

	@Value("${rdb.user}")
	private String rdbUserName;

	@Value("${rdb.password}")
	private String rdbPassword;

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

	@PostConstruct
	public void postConstruct() throws Exception {

		/*
		 * NOTE: All that's required to initialize for either mongodb or rdb, is to uncomment only
		 * one of these initializers (rdbInit or mongoInit). You should never call them both. Only
		 * call one.
		 */
		// rdbInit(rdbConnectionString, rdbUserName, rdbPassword);
		mongoInit(mongoDbHost, mongoDbPort, mongoDbName);

		UserManagerUtil.verifyAdminAccountReady(this);
		initRequiredNodes();
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
