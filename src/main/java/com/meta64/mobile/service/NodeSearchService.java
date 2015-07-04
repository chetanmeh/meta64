package com.meta64.mobile.service;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.model.NodeInfo;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.request.NodeSearchRequest;
import com.meta64.mobile.response.NodeSearchResponse;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.util.Convert;
import com.meta64.mobile.util.JcrUtil;

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

	/*
	 * see also: http://docs.jboss.org/jbossdna/0.7/manuals/reference/html/jcr-query-and-search.html
	 * https://wiki.magnolia-cms.com/display/WIKI/JCR+Query+Cheat+Sheet
	 * 
	 * Ordering example: 
	 * 
	 * final String statement = String.format(
	 * "SELECT * FROM [%s] WHERE ISDESCENDANTNODE('/%s') ORDER BY [jcr:content/jcr:lastModified]",
	 * context.getMap().get(ScalabilityBlobSearchSuite.CTX_FILE_NODE_TYPE_PROP), path);
	 */

	// see DescendantSearchTest
	/*
	 * TODO: need to escape the search text, and protect against any type of SQL-injection attack.
	 */
	public void search(Session session, NodeSearchRequest req, NodeSearchResponse res) throws Exception {

		int FETCH_NODES = 100;
		Node searchRoot = JcrUtil.findNode(session, req.getNodeId());

		QueryManager qm = session.getWorkspace().getQueryManager();
		String absPath = searchRoot.getPath();
		Query q = qm.createQuery("SELECT * from [nt:base] AS t WHERE ISDESCENDANTNODE([" + absPath + "]) AND contains(t.*, '" + req.getSearchText() + "')",
				Query.JCR_SQL2);
		QueryResult r = q.execute();
		NodeIterator nodes = r.getNodes();
		int counter = 0;
		List<NodeInfo> searchResults = new LinkedList<NodeInfo>();
		res.setSearchResults(searchResults);
		while (nodes.hasNext() && counter++ < FETCH_NODES) {
			searchResults.add(Convert.convertToNodeInfo(session, nodes.nextNode()));
		}
		res.setSuccess(true);
		log.debug("count: " + counter);
	}
}
