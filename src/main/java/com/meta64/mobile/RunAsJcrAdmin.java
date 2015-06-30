package com.meta64.mobile;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Helper class to run some processing workload as the admin user.
 */
@Component
@Scope("singleton")
public class RunAsJcrAdmin {

	@Autowired
	private OakRepositoryBean oak;

	public void run(JcrRunnable runner) throws Exception {
		Session session = null;

		try {
			session = oak.getRepository().login(new SimpleCredentials(oak.getJcrAdminUserName(), oak.getJcrAdminPassword().toCharArray()));
			runner.run(session);
		}
		finally {
			if (session != null) {
				session.logout();
				session = null;
			}
		}
	}
}
