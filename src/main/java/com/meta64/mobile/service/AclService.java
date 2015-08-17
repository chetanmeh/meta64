package com.meta64.mobile.service;

import java.security.Principal;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;

import org.apache.jackrabbit.oak.spi.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.oak.spi.security.principal.PrincipalImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.JcrProp;
import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.model.AccessControlEntryInfo;
import com.meta64.mobile.repo.OakRepository;
import com.meta64.mobile.request.AddPrivilegeRequest;
import com.meta64.mobile.request.GetNodePrivilegesRequest;
import com.meta64.mobile.request.RemovePrivilegeRequest;
import com.meta64.mobile.response.AddPrivilegeResponse;
import com.meta64.mobile.response.GetNodePrivilegesResponse;
import com.meta64.mobile.response.RemovePrivilegeResponse;
import com.meta64.mobile.user.AccessControlUtil;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.util.Convert;
import com.meta64.mobile.util.JcrUtil;

/**
 * Service methods for (ACL): processing security, privileges, and Access Control List information
 * on nodes.
 * 
 */
@Component
@Scope("singleton")
public class AclService {
	private static final Logger log = LoggerFactory.getLogger(AclService.class);

	@Autowired
	private OakRepository oak;

	@Autowired
	private SessionContext sessionContext;

	@Autowired
	private UserManagerService userManagerService;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	/**
	 * Returns the privileges that exist on the node identified in the request.
	 */
	public void getNodePrivileges(Session session, GetNodePrivilegesRequest req, GetNodePrivilegesResponse res) throws Exception {

		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);

		if (!req.isIncludeAcl() && !req.isIncludeOwners()) {
			throw new Exception("no specific information requested for getNodePrivileges");
		}

		res.setPublicAppend(JcrUtil.safeGetBooleanProp(node, JcrProp.PUBLIC_APPEND));

		if (req.isIncludeAcl()) {
			AccessControlEntry[] aclEntries = AccessControlUtil.getAccessControlEntries(session, node);
			List<AccessControlEntryInfo> aclEntriesInfo = Convert.convertToAclListInfo(aclEntries);
			res.setAclEntries(aclEntriesInfo);
		}

		if (req.isIncludeOwners()) {
			List<String> owners = userManagerService.getOwnerNames(node);
			res.setOwners(owners);
		}

		res.setSuccess(true);
	}

	/*
	 * I made this privilege capable of doing either a 'publicAppend' update, or actual privileges
	 * update. Only one at a time will be done, usually, if not always.
	 * 
	 * Adds a new privilege to a node. Request object is self explanatory.
	 */
	public void addPrivilege(Session session, AddPrivilegeRequest req, AddPrivilegeResponse res) throws Exception {

		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);
		JcrUtil.checkNodeCreatedBy(node, session.getUserID());

		boolean success = false;
		String principal = req.getPrincipal();
		if (principal != null) {
			List<String> privileges = req.getPrivileges();
			Principal principalObj = null;

			if (principal.equalsIgnoreCase(EveryonePrincipal.NAME)) {
				principalObj = EveryonePrincipal.getInstance();
			}
			else {
				principalObj = new PrincipalImpl(principal);
			}

			try {
				success = AccessControlUtil.grantPrivileges(session, node, principalObj, privileges);
			}
			catch (Exception e) {
				// leave success==false and continue.
			}
		}

		if (req.getPublicAppend() != null) {
			boolean publicAppend = req.getPublicAppend().booleanValue();
			if (!publicAppend) {
				JcrUtil.safeDeleteProperty(node, JcrProp.PUBLIC_APPEND);
			}
			else {
				node.setProperty(JcrProp.PUBLIC_APPEND, true);
			}
			success = true;
		}

		if (success) {
			session.save();
		}
		else {
			res.setMessage("Unable to alter privileges on node.");
		}
		res.setSuccess(success);
	}

	/*
	 * Removes the privilege specified in the request from the node specified in the request
	 */
	public void removePrivilege(Session session, RemovePrivilegeRequest req, RemovePrivilegeResponse res) throws Exception {
		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);
		JcrUtil.checkNodeCreatedBy(node, session.getUserID());

		String principal = req.getPrincipal();
		String privilege = req.getPrivilege();

		boolean success = AccessControlUtil.removeAclEntry(session, node, principal, privilege);
		session.save();
		res.setSuccess(success);
	}
}
