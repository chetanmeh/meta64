package com.meta64.mobile.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Manages certain aspects of Spring application context.
 */
@Component
@Scope("singleton")
public class SpringContextUtil implements ApplicationContextAware {
	private static final Logger log = LoggerFactory.getLogger(SpringContextUtil.class);

	private static ApplicationContext applicationContext;

	public void setApplicationContext(ApplicationContext context) throws BeansException {
		log.debug("SpringContextUtil initialized context.");
		applicationContext = context;
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public static Object getBean(Class clazz) {
		if (applicationContext == null) {
			throw new RuntimeException("SpringContextUtil accessed before spring initialized.");
		}

		return applicationContext.getBean(clazz);
	}

	public static Object getBean(String name) {
		if (applicationContext == null) {
			throw new RuntimeException("SpringContextUtil accessed before spring initialized.");
		}

		return applicationContext.getBean(name);
	}
}
