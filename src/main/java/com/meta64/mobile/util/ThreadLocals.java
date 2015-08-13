package com.meta64.mobile.util;

import javax.jcr.Session;

import org.pegdown.PegDownProcessor;

import com.meta64.mobile.response.base.OakResponseBase;

/**
 * Thread Local Storage
 * 
 * Note: We opt for ThreadLocals instead of a Spring Bean with Request scope, so that we can
 * decouple from Web Requests, and have these variables available on a *any* thread even if it's a
 * worker or deamon thread that isn't an actual Web Request. I never use "Request Scoping" unless
 * the object being scoped as request is specifically and solely something that exists only in an
 * actual web request.
 */
public class ThreadLocals {
	private static final ThreadLocal<Session> jcrSession = new ThreadLocal<Session>();
	private static final ThreadLocal<OakResponseBase> oakResponse = new ThreadLocal<OakResponseBase>();
	private static final ThreadLocal<PegDownProcessor> markdownProc = new ThreadLocal<PegDownProcessor>();
	
	public static void setJcrSession(Session session) {
		jcrSession.set(session);
	}

	public static Session getJcrSession() {
		return jcrSession.get();
	}

	public static void setMarkdownProc(PegDownProcessor session) {
		markdownProc.set(session);
	}

	public static PegDownProcessor getMarkdownProc() {
		return markdownProc.get();
	}
	
	public static void setResponse(OakResponseBase response) {
		oakResponse.set(response);
	}

	public static OakResponseBase getResponse() {
		return oakResponse.get();
	}
}
