package com.meta64.mobile.aspect;

import javax.jcr.Credentials;
import javax.jcr.GuestCredentials;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.config.SpringContextUtil;
import com.meta64.mobile.model.UserPreferences;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.request.LoginRequest;
import com.meta64.mobile.request.SignupRequest;
import com.meta64.mobile.response.LoginResponse;
import com.meta64.mobile.response.base.OakResponseBase;
import com.meta64.mobile.util.ThreadLocals;

/**
 * This is the core (and maybe only) chunk of AOP that we use in this app, that wraps the processing
 * of a REST call and handles all the boilerplate for performing a REST call on the server which
 * comes from the JQuery ajax calls from the client. Primarily we use the cross cutting concerns of
 * user login, and JCR session lifecycle.
 *
 */
@Aspect
@Component
public class OakSessionAspect {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private OakRepositoryBean oak;

	@Around("@annotation(com.meta64.mobile.annotate.OakSession)")
	public Object call(final ProceedingJoinPoint joinPoint) throws Throwable {

		Object ret = null;
		Session session = null;
		try {
			session = loginFromJoinPoint(joinPoint);

			ThreadLocals.setJcrSession(session);
			ret = joinPoint.proceed();
		}
		catch (Exception e) {
			log.error("exception: " + e.getMessage());

			/*
			 * if exception was thrown we get response from threadlocal, but really if we wanted to
			 * we should always be able to retrieve from threadlocal.
			 */
			ret = ThreadLocals.getResponse();

			if (ret == null) {
				ret = new OakResponseBase();
			}

			if (ret instanceof OakResponseBase) {
				OakResponseBase orb = (OakResponseBase) ret;
				if (orb != null) {
					orb.setSuccess(false);

					/* for now, we can just send back the actual exception message */
					orb.setMessage(e.getMessage());
				}
				else {
					log.error("Service method didn't set response object into threadlocals in exception case.");
				}
			}
		}
		finally {
			if (session != null) {
				session.logout();
				session = null;
			}

			/* cleanup this thread, servers reuse threads */
			ThreadLocals.setJcrSession(null);
			ThreadLocals.setResponse(null);
		}
		return ret;
	}

	/* Creates a logged in session for any method call for this join point */
	private Session loginFromJoinPoint(final ProceedingJoinPoint joinPoint) throws Exception {
		Object[] args = joinPoint.getArgs();
		String userName = "anonymous";
		String password = "anonymous";
		boolean usingCookies = false;

		Object req = (args != null && args.length > 0) ? args[0] : null;

		LoginResponse res = null;
		if (req instanceof LoginRequest) {
			res = new LoginResponse();
			res.setUserPreferences(new UserPreferences());
			ThreadLocals.setResponse(res);

			LoginRequest loginRequest = (LoginRequest) args[0];
			userName = loginRequest.getUserName();
			password = loginRequest.getPassword();
			usingCookies = loginRequest.isUsingCookies();

			if (userName.equals("{session}")) {
				SessionContext sessionContext = (SessionContext) SpringContextUtil.getBean(SessionContext.class);
				userName = sessionContext.getUserName();
				password = sessionContext.getPassword();
			}

			/* not logged in and page load is checking for logged in session */
			if (userName == null) {
				return null;
			}
		}
		else if (req instanceof SignupRequest) {
			/* we will have no session for user for signup request, so return null */
			return null;
		}
		else {
			SessionContext sessionContext = (SessionContext) SpringContextUtil.getBean(SessionContext.class);

			userName = sessionContext.getUserName();
			password = sessionContext.getPassword();

			if (userName == null) {
				userName = "anonymous";
			}
			if (password == null) {
				password = "anonymous";
			}
		}

		try {
			Credentials cred = userName.equals("anonymous") ? new GuestCredentials() : new SimpleCredentials(userName, password.toCharArray());
			Session session = oak.getRepository().login(cred);
			return session;
		}
		catch (Exception e) {
			if (res != null) {
				res.setSuccess(false);
				res.setMessage("Wrong username/password.");
			}
			throw e;
		}
	}
}
