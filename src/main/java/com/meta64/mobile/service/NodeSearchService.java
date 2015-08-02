package com.meta64.mobile.service;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.memory.PropertyStates;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.meta64.mobile.config.JcrProp;
import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.model.NodeInfo;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.request.NodeSearchRequest;
import com.meta64.mobile.response.NodeSearchResponse;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.util.Convert;
import com.meta64.mobile.util.JcrUtil;

/**
 * A lot of docs online had tricked me into believing that Oak is using indexes for all content by
 * default but this is not the case:
 * <p>
 * http://docs.adobe.com/docs/en/aem/6-0/deploy/upgrade/queries-and-indexing.html
 * 
 * http://jackrabbit.apache.org/oak/docs/query/lucene.html
 * http://users.jackrabbit.apache.narkive.com/6sQZPTKZ/no-results-from-full-text-index-oak-1-1-6
 * https://gist.github.com/chetanmeh/c1ccc4fa588ed1af467b
 * http://jackrabbit.510166.n4.nabble.com/Oak-Creating-Indexes-td4661890.html
 * <p>
 * Service for searching the repository. This searching is currently very basic, and just grabs the
 * first 100 results and returns.
 */
@Component
@Scope("singleton")
public class NodeSearchService {
	private static final Logger log = LoggerFactory.getLogger(NodeSearchService.class);

	@Autowired
	private OakRepositoryBean oak;

	@Autowired
	private SessionContext sessionContext;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	public Node findNodeByProperty(Session session, String parentPath, String propName, String propVal) throws Exception {
		QueryManager qm = session.getWorkspace().getQueryManager();

		StringBuilder queryStr = new StringBuilder();
		queryStr.append("SELECT * from [nt:base] AS t WHERE ISDESCENDANTNODE([");
		queryStr.append(parentPath);
		// TODO: figure out how to do equals instead of contains here!
		queryStr.append("]) AND contains(t." + propName + ", '");
		queryStr.append(propVal);
		queryStr.append("')");

		// if (req.isModSortDesc()) {
		// queryStr.append(" ORDER BY [jcr:lastModified] DESC");
		// }

		Query q = qm.createQuery(queryStr.toString(), Query.JCR_SQL2);
		QueryResult r = q.execute();
		NodeIterator nodes = r.getNodes();
		Node ret = null;
		if (nodes.hasNext()) {
			ret = nodes.nextNode();
		}
		return ret;
	}

	/*
	 * see also: http://docs.jboss.org/jbossdna/0.7/manuals/reference/html/jcr-query-and-search.html
	 * https://wiki.magnolia-cms.com/display/WIKI/JCR+Query+Cheat+Sheet
	 */

	// see DescendantSearchTest
	public void search(Session session, NodeSearchRequest req, NodeSearchResponse res) throws Exception {

		int MAX_NODES = 100;
		Node searchRoot = JcrUtil.findNode(session, req.getNodeId());

		QueryManager qm = session.getWorkspace().getQueryManager();
		String absPath = searchRoot.getPath();

		StringBuilder queryStr = new StringBuilder();
		queryStr.append("SELECT * from [nt:base] AS t WHERE ");

		int whereCount = 0;
		if (!absPath.equals("/")) {
			whereCount++;
			queryStr.append("ISDESCENDANTNODE([");
			queryStr.append(absPath);
			queryStr.append("])");
		}

		if (req.getSearchText().length() > 0) {
			/*
			 * To search ALL properties you can put 't.*' instead of 't.[jcr:content]' below.
			 */
			if (whereCount > 0) {
				queryStr.append(" AND ");
			}
			whereCount++;
			queryStr.append("contains(t.[");
			queryStr.append(JcrProp.CONTENT);
			queryStr.append("], '");
			queryStr.append(escapeQueryString(req.getSearchText()));
			queryStr.append("')");
		}

		if (req.isModSortDesc()) {
			queryStr.append(" ORDER BY [");
			queryStr.append(JcrProp.LAST_MODIFIED);
			queryStr.append("] DESC");
		}

		Query q = qm.createQuery(queryStr.toString(), Query.JCR_SQL2);
		QueryResult r = q.execute();
		NodeIterator nodes = r.getNodes();
		int counter = 0;
		List<NodeInfo> searchResults = new LinkedList<NodeInfo>();
		res.setSearchResults(searchResults);

		while (nodes.hasNext()) {
			searchResults.add(Convert.convertToNodeInfo(sessionContext, session, nodes.nextNode()));
			if (counter++ > MAX_NODES) {
				break;
			}
		}
		res.setSuccess(true);
		log.debug("search results count: " + counter);
	}

	private String escapeQueryString(String query) {
		return query.replaceAll("'", "''");
	}

	/*
	 * UGH! This was one mess of an experiment all day, after which I decided to move to the latest
	 * branch of Oak (1.0.18), and it's working, but I still have not yet determined if we need 
	 * to be doing all this "QueryIndexDefinition" stuff on the latest oak or not.
	 */
	public void initLuceneRepoNodes(Session session) throws Exception {
	
//		NodeBuilder index = oak.getRoot().builder().child("oak:index");
//		  index.child("lucene")
//		    .setProperty("jcr:primaryType", "oak:QueryIndexDefinition", Type.NAME)
//		    .setProperty("type", "lucene")
//		    .setProperty("async", "async")
//		    .setProperty(PropertyStates.createProperty("includePropertyTypes", ImmutableSet.of(
//		        PropertyType.TYPENAME_STRING, PropertyType.TYPENAME_BINARY), Type.STRINGS))
//		    .setProperty(PropertyStates.createProperty("excludePropertyNames", ImmutableSet.of( 
//		        "jcr:createdBy", "jcr:lastModifiedBy"), Type.STRINGS))
//		    .setProperty("reindex", true);
//		  
		//////////////////
//		
//		String indexName = "lucene";
//		Node luceneIndexNode = JcrUtil.ensureNodeExists(session, "/oak:index", indexName, null, "nt:unstructured", false);
//		//luceneIndexNode.setProperty("jcr:primaryType", "oak:QueryIndexDefinition");
//		luceneIndexNode.setProperty("jcr:primaryType", "oak:QueryIndexDefinition", PropertyType.NAME);
//		//luceneIndexNode.setProperty("propertyNames", new String[] { "jcr:content" });
//		luceneIndexNode.setProperty("includePropertyTypes", new String[] {
//		        PropertyType.TYPENAME_STRING}, PropertyType.STRING);
//		//luceneIndexNode.setProperty("compatVersion", 2);
//		luceneIndexNode.setProperty("type", "lucene");
//		luceneIndexNode.setProperty("async", "async");
//		//luceneIndexNode.setProperty("reindex-async", true);
//		luceneIndexNode.setProperty("reindex", true);
//
//		Node indexRules = JcrUtil.ensureNodeExists(session, "/oak:index/"+indexName, "indexRules", null);
//		//indexRules.setProperty("jcr:primaryType", "nt:unstructured");
//
//		Node ntBase = JcrUtil.ensureNodeExists(session, "/oak:index/"+indexName+"/indexRules", "nt:base", null);
//		Node properties = JcrUtil.ensureNodeExists(session, "/oak:index/"+indexName+"/indexRules/nt:base", "properties", null);
//		//properties.setProperty("jcr:primaryType", "nt:unstructured");
//
//		Node colNode = JcrUtil.ensureNodeExists(session, "/oak:index/"+indexName+"/indexRules/nt:base/properties", "jcr:content", null);
//		colNode.setProperty("propertyIndex", true);
//		colNode.setProperty("name", "jcr:content");

		session.save();
	}
}
