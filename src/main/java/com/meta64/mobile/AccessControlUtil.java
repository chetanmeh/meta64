package com.meta64.mobile;

import java.security.Principal;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for changing access controls on nodes. This means, who can read nodes, modify
 * nodes, etc. Standard access privileges.
 */
public class AccessControlUtil {
	private static final Logger log = LoggerFactory.getLogger(AccessControlUtil.class);

	public static String interpretPrivilegeName(String name) {
		if (name.equalsIgnoreCase("read") || name.equalsIgnoreCase("jcr:read")) {
			return Privilege.JCR_READ;
		}
		return name;
	}

	public static Privilege[] makePrivilegesFromNames(AccessControlManager acMgr, String[] names) throws Exception {
		Privilege[] privileges = new Privilege[names.length];
		int idx = 0;
		for (String name : names) {
			name = interpretPrivilegeName(name);
			privileges[idx++] = acMgr.privilegeFromName(name);
		}
		return privileges;
	}

	public static boolean grantPrivileges(Session session, Node node, Principal principal, String... privilegeNames) throws Exception {

		AccessControlManager acMgr = session.getAccessControlManager();
		AccessControlList acl = getAccessControlList(session, node);

		if (acl != null) {
			Privilege[] privileges = makePrivilegesFromNames(acMgr, privilegeNames);
			acl.addAccessControlEntry(principal, privileges);
			acMgr.setPolicy(node.getPath(), (AccessControlPolicy) acl);
			return true;
		}

		return false;
	}

	public static AccessControlEntry[] getAccessControlEntries(Session session, Node node) throws Exception {
		AccessControlList acl = getAccessControlList(session, node);
		return acl != null ? acl.getAccessControlEntries() : null;
	}

	public static AccessControlList getAccessControlList(Session session, Node node) throws Exception {

		String path = node.getPath();
		AccessControlManager acMgr = session.getAccessControlManager();

		AccessControlPolicyIterator iter = acMgr.getApplicablePolicies(path);
		while (iter.hasNext()) {
			AccessControlPolicy policy = iter.nextAccessControlPolicy();
			// log.debug("policy: " + policy.getClass().getName());

			if (policy instanceof AccessControlList) {
				return (AccessControlList) policy;
			}
		}

		AccessControlPolicy[] list = acMgr.getPolicies(path);
		for (AccessControlPolicy policy : list) {
			// log.debug("policy: " + policy.getClass().getName());

			if (policy instanceof AccessControlList) {
				return (AccessControlList) policy;
			}
		}

		/* No access control list found */
		return null;
	}

	/* search for removePolicy in commented code below for a better way to do this */
	public static boolean removeAclEntry(Session session, Node node, String principle, String privilege) throws Exception {
		boolean policyChanged = false;
		String path = node.getPath();

		AccessControlManager acMgr = session.getAccessControlManager();
		log.debug("Privileges for node: " + path + " ");

		AccessControlList acl = getAccessControlList(session, node);
		AccessControlEntry[] aclArray = acl.getAccessControlEntries();
		log.debug("ACL entry count: " + (aclArray == null ? 0 : aclArray.length));

		for (AccessControlEntry ace : aclArray) {

			/*
			 * TODO: each ace has multiple privileges, so we need to detect if we have removed all
			 * privileges and, then in that case call
			 */
			log.debug("ACL entry (principal name): " + ace.getPrincipal().getName());
			if (ace.getPrincipal().getName().equals(principle)) {
				log.debug("  Found PRINCIPLE to remove priv for: " + principle);
				Privilege[] privileges = ace.getPrivileges();

				if (privileges != null) {
					for (Privilege priv : privileges) {
						if (priv.getName().equals(privilege)) {
							log.debug("    Found PRIVILEGE to remove: " + principle);

							/*
							 * we remove the entire 'ace' from the 'acl' here. I don't know of a
							 * more find-grained way to remove privileges than to remove entire
							 * 'ace' which can have multiple privileges on it. :(
							 */
							acl.removeAccessControlEntry(ace);
							policyChanged = true;

							/* break out of privileges scanning, this entire 'ace' is dead now */
							break;
						}
					}
				}
			}

			if (policyChanged) {
				acMgr.setPolicy(path, (AccessControlPolicy) acl);
			}
		}
		return policyChanged;
	}

	public static boolean grantFullAccess(Session session, Node node, final String ownerName) throws Exception {
		Principal principal = new CustomPrincipal(ownerName);
		return grantPrivileges(session, node, principal, Privilege.JCR_ALL);
	}

	//
	// /*
	// *
	// * DO NOT DELETE
	// */
	//
	// // Privilege[] privileges = acMgr.getPrivileges(node.getPath());
	// // log.debug("getPrivileges=" + (privileges == null ? 0 : privileges.length));
	// //
	// // AccessControlPolicy[] effectivePolicies = acMgr.getEffectivePolicies(node.getPath());
	// // log.debug("getEffectivePolicies=" + (effectivePolicies == null ? 0 :
	// // effectivePolicies.length));
	// //
	// // Privilege[] supportedPrivileges = acMgr.getSupportedPrivileges(node.getPath());
	// // log.debug("getSupportedPrivileges=" + (supportedPrivileges == null ? 0 :
	// // supportedPrivileges.length));
	//
	// // AccessControlPolicyIterator iter = acMgr.getApplicablePolicies(node.getPath());
	// // int appliciablePolicyCount = 0;
	// // while (iter.hasNext()) {
	// // appliciablePolicyCount++;
	// //

	/* commenting, because this can be rewritten better now */
	// see oak's: AbstractSecurityTest.java, ACLTest.java
	// public static String dumpPrivileges(Session session, Node node) throws Exception {
	// StringBuilder sb = new StringBuilder();
	// AccessControlManager acMgr = session.getAccessControlManager();
	// sb.append("Privileges for node: " + node.getPath() + " ");
	//
	// // int policyCounter = 0;
	// // int aclCounter = 0;
	// // for (AccessControlPolicy policy : acMgr.getPolicies(node.getPath())) {
	// //
	// // if (policy instanceof AccessControlList) {
	// // aclCounter = 0;
	// //
	// // AccessControlList acl = (AccessControlList) policy;
	// AccessControlEntry[] aclEntries = acl.getAccessControlEntries();
	// if (aclEntries != null) {
	// for (AccessControlEntry aclEntry : aclEntries) {
	// Principal principal = aclEntry.getPrincipal();
	// sb.append("ACE - Principal: " + principal.getName() + " Grants: ");
	// Privilege[] privileges = aclEntry.getPrivileges();
	// if (privileges != null) {
	// int counter = 0;
	// for (Privilege privilege : privileges) {
	// if (counter > 0) {
	// sb.append(",");
	// }
	// sb.append(privilege.getName());
	// counter++;
	// }
	// }
	// }
	// }
	// // }
	// // policyCounter++;
	// // }
	//
	// // if (policyCounter == 0 || aclCounter == 0) {
	// // sb.append("no policies.");
	// // }
	// return sb.toString();
	//
	// // AccessControlManager session.getAccessControlManager();
	// // AccessControlManager acMgr = getAccessControlManager(node);
	// // AccessControlList acl = AccessControlUtils.getAccessControlList(acMgr, "/content");
	// // acl.addAccessControlEntry(getTestUser().getPrincipal(), privilegesFromNames(
	// // PrivilegeConstants.JCR_READ));
	// //
	// // ////////// Node target = testRootNode.addNode("test",
	// // "test:sameNameSibsFalseChildNodeDefinition");
	// // AccessControlManager acMgr = superuser.getAccessControlManager();
	// // for (AccessControlPolicyIterator it = acMgr.getApplicablePolicies(target.getPath());
	// // it.hasNext(); ) {
	// // AccessControlPolicy policy = it.nextAccessControlPolicy();
	// // if (policy instanceof AccessControlList) {
	// // if (principal != null) {
	// // Privilege[] privs = new
	// // Privilege[]{acMgr.privilegeFromName(Privilege.JCR_LOCK_MANAGEMENT)};
	// // ((AccessControlList) policy).addAccessControlEntry(principal, privs);
	// // }
	// // acMgr.setPolicy(target.getPath(), policy);
	// // }
	// // }
	// // if (!isSessionImport()) {
	// // superuser.save();
	// // }
	// // ///////////////
	// // private AccessControlList getList(@Nullable String path) throws RepositoryException {
	// // if (path == null || superuser.nodeExists(path)) {
	// // for (AccessControlPolicy policy : acMgr.getPolicies(path)) {
	// // if (policy instanceof AccessControlList) {
	// // return (AccessControlList) policy;
	// // }
	// // }
	// // }
	// // return null;
	// // }
	// // ---
	// // AccessControlList list = getList(path); //pasted above
	// // if (list != null) {
	// // if (remove) {
	// // acMgr.removePolicy(path, list);
	// // } else {
	// // for (AccessControlEntry ace : list.getAccessControlEntries()) {
	// // list.removeAccessControlEntry(ace);
	// // }
	// // for (AccessControlEntry ace : entries) {
	// // list.addAccessControlEntry(ace.getPrincipal(), ace.getPrivileges());
	// // }
	// // acMgr.setPolicy(path, list);
	// // }
	// // }
	// }

	public static String[] namesFromPrivileges(Privilege... privileges) {
		if (privileges == null || privileges.length == 0) {
			return new String[0];
		}
		else {
			String[] names = new String[privileges.length];
			for (int i = 0; i < privileges.length; i++) {
				names[i] = privileges[i].getName();
			}
			return names;
		}
	}
}
