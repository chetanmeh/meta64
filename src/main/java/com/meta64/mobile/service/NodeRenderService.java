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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.Convert;
import com.meta64.mobile.JcrUtil;
import com.meta64.mobile.Log;
import com.meta64.mobile.OakRepositoryBean;
import com.meta64.mobile.RunAsJcrAdmin;
import com.meta64.mobile.SessionContext;
import com.meta64.mobile.model.NodeInfo;
import com.meta64.mobile.request.RenderNodeRequest;
import com.meta64.mobile.response.RenderNodeResponse;

/**
 * Service for rendering the content of a page.
 */
@Component
@Scope("session")
public class NodeRenderService {
	private static final Logger log = LoggerFactory.getLogger(NodeRenderService.class);

	@Autowired
	private OakRepositoryBean oak;

	@Autowired
	private SessionContext sessionContext;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	public void renderNode(Session session, RenderNodeRequest req, RenderNodeResponse res) throws Exception {

		List<NodeInfo> children = new LinkedList<NodeInfo>();
		res.setChildren(children);

		Node node = JcrUtil.findNode(session, req.getNodeId());

		int levelsUpRemaining = req.getUpLevel();
		while (node != null && levelsUpRemaining > 0) {
			node = node.getParent();
			if (Log.renderNodeRequest) {
				// System.out.println("   upLevel to nodeid: "+item.getPath());
			}
			levelsUpRemaining--;
		}

		NodeInfo nodeInfo = Convert.convertToNodeInfo(session, node);
		NodeType type = node.getPrimaryNodeType();
		boolean ordered = type.hasOrderableChildNodes();
		nodeInfo.setChildrenOrdered(ordered);
		// System.out.println("Primary type: " + type.getName() + " childrenOrdered=" +
		// ordered);
		res.setNode(nodeInfo);

		NodeIterator nodeIter = node.getNodes();
		try {
			while (true) {
				Node n = nodeIter.nextNode();
				children.add(Convert.convertToNodeInfo(session, n));
			}
		}
		catch (NoSuchElementException ex) {
			// not an error. Normal iterator end condition.
		}
	}
}
