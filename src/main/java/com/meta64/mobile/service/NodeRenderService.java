package com.meta64.mobile.service;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.JcrProp;
import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.model.NodeInfo;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.request.AnonPageLoadRequest;
import com.meta64.mobile.request.RenderNodeRequest;
import com.meta64.mobile.response.AnonPageLoadResponse;
import com.meta64.mobile.response.RenderNodeResponse;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.user.UserSettingsDaemon;
import com.meta64.mobile.util.Convert;
import com.meta64.mobile.util.JcrUtil;
import com.meta64.mobile.util.Log;
import com.meta64.mobile.util.XString;

/**
 * Service for rendering the content of a page. The actual page is not rendered on the server side.
 * What we are really doing here is generating a list of POJOS that get converted to JSON and sent
 * to the client. But regardless of format this is the primary service for pulling content up for
 * rendering the pages on the client as the user browses around on the tree.
 */
@Component
@Scope("singleton")
public class NodeRenderService {
	private static final Logger log = LoggerFactory.getLogger(NodeRenderService.class);

	private boolean debug = false;

	@Value("${anonUserLandingPageNode}")
	private String anonUserLandingPageNode;

	@Autowired
	private OakRepositoryBean oak;

	@Autowired
	private UserSettingsDaemon userSettingsDaemon;

	@Autowired
	private SessionContext sessionContext;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	/*
	 * This is the call that gets all the data to show on a page. Whenever user is browsing to a new
	 * page, this method gets called once per page and retrieves all the data for that page.
	 * 
	 * TODO: When 'edit mode' is not *on* on the client side, we probably should have a way of
	 * passing a parameter to this method that retrieves *less than all* (i.e. not all, but a
	 * subset) of the information for each node, so that rendering can be much faster. Currently
	 * it's already lightning fast, but for scalability we need to be returning basically the least
	 * amount of data that we can possible get away with.
	 */
	public void renderNode(Session session, RenderNodeRequest req, RenderNodeResponse res, boolean allowRootAutoPrefix) throws Exception {

		List<NodeInfo> children = new LinkedList<NodeInfo>();
		res.setChildren(children);

		String targetId = req.getNodeId();

		if (debug) {
			log.debug("renderNode targetId:" + targetId);
		}

		Node node = JcrUtil.safeFindNode(session, targetId);

		/*
		 * if the node was a path type and was not found then try with the "/root" prefix before
		 * giving up. We allow ID parameters to omit the leading "/root" part of the path for
		 * shortening the path just for end user convenience.
		 */
		if (node == null && targetId.startsWith("/") && allowRootAutoPrefix) {
			targetId = "/root" + targetId;
			node = JcrUtil.safeFindNode(session, targetId);
		}

		if (node == null) {
			res.setMessage("Node not found.");
			res.setSuccess(false);
			return;
		}

		if (debug) {
			log.debug("found node:" + targetId);
		}

		String path = node.getPath();
		userSettingsDaemon.setSettingVal(sessionContext.getUserName(), JcrProp.USER_PREF_LAST_NODE, path);

		if (req.isRenderParentIfLeaf() && !node.hasNodes() /* Convert.hasDisplayableNodes(node) */) {
			res.setDisplayedParent(true);
			req.setUpLevel(1);
		}

		int levelsUpRemaining = req.getUpLevel();
		while (node != null && levelsUpRemaining > 0) {
			node = node.getParent();
			if (Log.renderNodeRequest) {
				// log.debug("   upLevel to nodeid: "+item.getPath());
			}
			levelsUpRemaining--;
		}

		NodeInfo nodeInfo = Convert.convertToNodeInfo(sessionContext, session, node);
		NodeType type = node.getPrimaryNodeType();
		boolean ordered = type.hasOrderableChildNodes();
		nodeInfo.setChildrenOrdered(ordered);
		// log.debug("Primary type: " + type.getName() + " childrenOrdered=" +
		// ordered);
		res.setNode(nodeInfo);

		NodeIterator nodeIter = node.getNodes();
		try {
			int nodeCount = 0;
			if (debug) {
				log.debug("starting iteration of children.");
			}
			while (true) {
				Node n = nodeIter.nextNode();
				children.add(Convert.convertToNodeInfo(sessionContext, session, n));

				/*
				 * Instead of crashing browser with too much load, just fail a bit more gracefully
				 * when the limits of this application are exceeded.
				 */
				if (++nodeCount > 1000) {
					throw new Exception("Node has too many children (> 1000)");
				}

				if (debug) {
					log.debug("child: " + nodeCount);
				}
			}
		}
		catch (NoSuchElementException ex) {
			// not an error. Normal iterator end condition.
		}
	}

	/*
	 * There is a system defined way for admins to specify what node should be displayed in the
	 * browser when a non-logged in user (i.e. anonymouse user) is browsing the site, and this
	 * method retrieves that page data.
	 */
	public void anonPageLoad(Session session, AnonPageLoadRequest req, AnonPageLoadResponse res) throws Exception {

		boolean allowRootAutoPrefix = false;
		String id = null;
		if (id == null) {
			if (!req.isIgnoreUrl() && sessionContext.getUrlId() != null) {
				id = sessionContext.getUrlId();
				allowRootAutoPrefix = true;
			}
			else {
				id = anonUserLandingPageNode;
			}
		}

		if (!XString.isEmpty(id)) {
			RenderNodeResponse renderNodeRes = new RenderNodeResponse();
			RenderNodeRequest renderNodeReq = new RenderNodeRequest();

			/*
			 * if user specified an ID= parameter on the url, we display that immediately, or else
			 * we display the node that the admin has configured to be the default landing page
			 * node.
			 */
			renderNodeReq.setNodeId(id);
			renderNode(session, renderNodeReq, renderNodeRes, allowRootAutoPrefix);
			res.setRenderNodeResponse(renderNodeRes);
		}
		else {
			res.setContent("No content available.");
		}

		res.setSuccess(true);
	}
}
