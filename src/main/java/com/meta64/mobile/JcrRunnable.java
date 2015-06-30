package com.meta64.mobile;

import javax.jcr.Session;

/**
 * Runs a unit of work in a specific JCR session.
 */
public abstract class JcrRunnable {
	public abstract void run(Session session) throws Exception;
}
