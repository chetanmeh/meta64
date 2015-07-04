package com.meta64.mobile.service;

import javax.jcr.Node;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.JcrUtil;
import com.meta64.mobile.OakRepositoryBean;
import com.meta64.mobile.RunAsJcrAdmin;
import com.meta64.mobile.SessionContext;
import com.meta64.mobile.request.NodeSearchRequest;
import com.meta64.mobile.response.NodeSearchResponse;

/**
 * Service for searching the repository
 */
@Component
@Scope("session")
public class NodeSearchService {
	private static final Logger log = LoggerFactory.getLogger(NodeSearchService.class);

	@Autowired
	private OakRepositoryBean oak;

	@Autowired
	private SessionContext sessionContext;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	public void search(Session session, NodeSearchRequest req, NodeSearchResponse res) throws Exception {

		Node node = JcrUtil.findNode(session, req.getNodeId());
	}
}
