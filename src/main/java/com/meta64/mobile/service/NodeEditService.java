package com.meta64.mobile.service;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.model.PropertyInfo;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.request.CreateSubNodeRequest;
import com.meta64.mobile.request.DeletePropertyRequest;
import com.meta64.mobile.request.InsertNodeRequest;
import com.meta64.mobile.request.MakeNodeReferencableRequest;
import com.meta64.mobile.request.SaveNodeRequest;
import com.meta64.mobile.request.SavePropertyRequest;
import com.meta64.mobile.response.CreateSubNodeResponse;
import com.meta64.mobile.response.DeletePropertyResponse;
import com.meta64.mobile.response.InsertNodeResponse;
import com.meta64.mobile.response.MakeNodeReferencableResponse;
import com.meta64.mobile.response.SaveNodeResponse;
import com.meta64.mobile.response.SavePropertyResponse;
import com.meta64.mobile.util.Convert;
import com.meta64.mobile.util.JcrUtil;
import com.meta64.mobile.util.XString;

/**
 * Service for editing content of nodes.
 */
@Component
@Scope("session")
public class NodeEditService {
	private static final Logger log = LoggerFactory.getLogger(NodeEditService.class);

	@Autowired
	private OakRepositoryBean oak;

	@Autowired
	private SessionContext sessionContext;

	public void createSubNode(Session session, CreateSubNodeRequest req, CreateSubNodeResponse res) throws Exception {
		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);
		// session.checkPermission(absPath, actions);

		String name = XString.isEmpty(req.getNewNodeName()) ? JcrUtil.getGUID() : req.getNewNodeName();

		/* NT_UNSTRUCTURED IS ORDERABLE */
		Node newNode = node.addNode(name, JcrConstants.NT_UNSTRUCTURED);
		newNode.setProperty("jcr:content", "");
		JcrUtil.timestampNewNode(session, newNode);
		session.save();

		res.setNewNode(Convert.convertToNodeInfo(sessionContext, session, newNode));
		res.setSuccess(true);
	}

	public void insertNode(Session session, InsertNodeRequest req, InsertNodeResponse res) throws Exception {

		String parentNodeId = req.getParentId();
		// System.out.println("Inserting under parent: " + parentNodeId);
		Node parentNode = JcrUtil.findNode(session, parentNodeId);

		String name = XString.isEmpty(req.getNewNodeName()) ? JcrUtil.getGUID() : req.getNewNodeName();

		/* NT_UNSTRUCTURED IS ORDERABLE */
		Node newNode = parentNode.addNode(name, JcrConstants.NT_UNSTRUCTURED);
		newNode.setProperty("jcr:content", "");
		JcrUtil.timestampNewNode(session, newNode);
		session.save();

		if (!XString.isEmpty(req.getTargetName())) {
			parentNode.orderBefore(newNode.getName(), req.getTargetName());
		}

		session.save();
		res.setNewNode(Convert.convertToNodeInfo(sessionContext, session, newNode));
		res.setSuccess(true);
	}

	public void saveProperty(Session session, SavePropertyRequest req, SavePropertyResponse res) throws Exception {
		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);
		node.setProperty(req.getPropertyName(), req.getPropertyValue());
		session.save();

		PropertyInfo propertySaved = new PropertyInfo(-1, req.getPropertyName(), req.getPropertyValue(), null);
		res.setPropertySaved(propertySaved);
		res.setSuccess(true);
	}

	public void makeNodeReferencable(Session session, MakeNodeReferencableRequest req, MakeNodeReferencableResponse res) throws Exception {
		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);

		if (node != null) {
			/* if node already has uuid then we can do nothing here, we just silently return success */
			if (!node.hasProperty("jcr:uuid")) {
				node.addMixin(JcrConstants.MIX_REFERENCEABLE);
				session.save();
			}
			res.setSuccess(true);
		}
	}

	public void saveNode(Session session, SaveNodeRequest req, SaveNodeResponse res) throws Exception {
		String nodeId = req.getNodeId();
		// log.debug("saveNode. nodeId=" + nodeId);
		Node node = JcrUtil.findNode(session, nodeId);

		if (req.getProperties() != null) {
			for (PropertyInfo property : req.getProperties()) {

				/*
				 * save only if server determines the property is savable. Just protection. Client
				 * shouldn't be trying to save stuff that is illegal to save, but we have to assume
				 * the worst behavior from client code, for security and robustness.
				 */
				if (JcrUtil.isSavableProperty(property.getName())) {
					log.debug("Property to save: " + property.getName() + "=" + property.getValue());
					node.setProperty(property.getName(), property.getValue());
				}
				else {
					log.debug("Ignoring rogue save attempt on prop: " + property.getName());
				}
			}

			Calendar lastModified = Calendar.getInstance();
			node.setProperty("jcr:lastModified", lastModified);
			node.setProperty("jcr:lastModifiedBy", session.getUserID());

			session.save();
		}
		res.setSuccess(true);
	}

	public void deleteProperty(Session session, DeletePropertyRequest req, DeletePropertyResponse res) throws Exception {
		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);

		String propertyName = req.getPropName();
		try {
			Property prop = node.getProperty(propertyName);
			if (prop != null) {
				// System.out.println("Deleting property: " + propertyName);
				prop.remove();
			}
			else {
				throw new Exception("Unable to find property to delete: " + propertyName);
			}
		}
		catch (Exception e) {
			/*
			 * Don't rethrow this exception. We want to keep processing any properties we can
			 * successfully process
			 */
			log.info("Failed to delete property: " + propertyName + " Reason: " + e.getMessage());
		}

		session.save();
		res.setSuccess(true);
	}
}
