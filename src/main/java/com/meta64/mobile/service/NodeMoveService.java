package com.meta64.mobile.service;

import javax.jcr.Node;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.request.DeleteNodesRequest;
import com.meta64.mobile.request.MoveNodesRequest;
import com.meta64.mobile.request.SetNodePositionRequest;
import com.meta64.mobile.response.DeleteNodesResponse;
import com.meta64.mobile.response.MoveNodesResponse;
import com.meta64.mobile.response.SetNodePositionResponse;
import com.meta64.mobile.util.JcrUtil;

/**
 * Service for editing content of nodes.
 */
@Component
@Scope("session")
public class NodeMoveService {
	private static final Logger log = LoggerFactory.getLogger(NodeMoveService.class);

	@Autowired
	private OakRepositoryBean oak;

	@Autowired
	private SessionContext sessionContext;

	public void setNodePosition(Session session, SetNodePositionRequest req, SetNodePositionResponse res) throws Exception {
		String parentNodeId = req.getParentNodeId();
		Node parentNode = JcrUtil.findNode(session, parentNodeId);
		parentNode.orderBefore(req.getNodeId(), req.getSiblingId());
		session.save();
		res.setSuccess(true);
	}

	public void deleteNodes(Session session, DeleteNodesRequest req, DeleteNodesResponse res) throws Exception {

		for (String nodeId : req.getNodeIds()) {
			log.debug("Deleting ID: " + nodeId);
			try {
				Node node = JcrUtil.findNode(session, nodeId);
				node.remove();
			}
			catch (Exception e) {
				// silently ignore if node cannot be found.
			}
		}
		session.save();
		res.setSuccess(true);
	}

	public void moveNodes(Session session, MoveNodesRequest req, MoveNodesResponse res) throws Exception {
		String targetId = req.getTargetNodeId();
		Node targetNode = JcrUtil.findNode(session, targetId);
		String targetPath = targetNode.getPath();
		// String targetChildId = req.getTargetChildId();

		for (String nodeId : req.getNodeIds()) {
			log.debug("Moving ID: " + nodeId);
			try {
				Node node = JcrUtil.findNode(session, nodeId);

				/*
				 * This code moves the copied nodes to the bottom of child list underneath the
				 * target node (i.e. targetNode being the parent) for the new node locations.
				 */

				String srcPath = node.getPath();
				String dstPath = targetPath + "/" + node.getName();
				// log.debug("MOVE: srcPath[" + srcPath + "] targetPath[" + dstPath + "]");
				session.move(srcPath, dstPath);
				// session.save();

				/*
				 * This code did not work as expected (or at all). This is supposed to move the new
				 * nodes into the proper ordinal position, and doesn't work. Since this is lower
				 * priority, i'm not even going to try to figure this out for now, and will just
				 * leave it as technical debt, TODO
				 */
				// if (targetChildId != null) {
				// targetNode.orderBefore(dstPath, targetChildId);
				// //session.save();
				// }
			}
			catch (Exception e) {
				// silently ignore if node cannot be found.
			}
		}
		session.save();
		res.setSuccess(true);
	}
}
