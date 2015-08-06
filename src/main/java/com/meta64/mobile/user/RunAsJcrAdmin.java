package com.meta64.mobile.user;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.util.JcrRunnable;

/**
 * Helper class to run some processing workload as the admin user. Simplifies by encapsulating the
 * session management at this abstracted layer.
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
			}
		}
	}
}
