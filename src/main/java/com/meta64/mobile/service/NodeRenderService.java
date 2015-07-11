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

import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.model.NodeInfo;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.request.RenderNodeRequest;
import com.meta64.mobile.response.RenderNodeResponse;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.util.Convert;
import com.meta64.mobile.util.JcrUtil;
import com.meta64.mobile.util.Log;

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

		String targetId = req.getNodeId();
		
		Node node; 
		try {
			node = JcrUtil.findNode(session, targetId);
		}
		catch (Exception e) {
			res.setMessage("Node not found.");
			res.setSuccess(false);
			return;
		}

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
