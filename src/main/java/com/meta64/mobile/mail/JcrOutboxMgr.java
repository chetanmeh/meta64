package com.meta64.mobile.mail;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.ConstantsProvider;
import com.meta64.mobile.config.JcrName;
import com.meta64.mobile.config.JcrProp;
import com.meta64.mobile.repo.OakRepository;
import com.meta64.mobile.service.UserManagerService;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.util.JcrRunnable;
import com.meta64.mobile.util.JcrUtil;

/**
 * Meta64 has a node where it stores all emails that are queued up to be sent for whatever reason.
 * Emails related to User Signups in progress can be cached on the tree, or email notifications
 * because of node editing can also be cached on the tree. So basically this is a persistent queue
 * of emails that are ready to be sent. This class manages storing the emails on the tree. There are
 * other classes that handle the actual sending of the messages, and a background deamon thread that
 * periodically checks for emails ready to be sent and sends them.
 */
@Component
@Scope("singleton")
public class JcrOutboxMgr {

	private static final Logger log = LoggerFactory.getLogger(JcrOutboxMgr.class);

	@Autowired
	private OakRepository oak;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	@Autowired
	private ConstantsProvider constProvider;

	/*
	 * node=Node that was created. userName = username of person who just created node.
	 */
	public void sendNotificationForChildNodeCreate(final Node node, final String userName) throws Exception {
		/*
		 * put in a catch block, because nothing going wrong in here should be allowed to blow up
		 * the save operation
		 */
		adminRunner.run(new JcrRunnable() {
			@Override
			public void run(Session session) throws Exception {
				try {
					Node parentNode = node.getParent();
					if (parentNode != null) {
						String parentCreator = JcrUtil.getRequiredStringProp(parentNode, JcrProp.CREATED_BY);
						if (!parentCreator.equals(userName)) { // sessionContext.getUserName())) {
							Node prefsNode = UserManagerService.getPrefsNodeForSessionUser(session, parentCreator);
							String email = JcrUtil.getRequiredStringProp(prefsNode, JcrProp.EMAIL);
							log.debug("TODO: send email to: " + email + " because his node was appended under.");

							String content = String.format("User '%s' has created a new subnode under one of your nodes.<br>\n\n" + //
									"Here is a link to the new node: %s?id=%s", //
									userName, constProvider.getHostAndPort(), node.getPath());

							queueMailUsingAdminSession(session, email, "Meta64 New Content Nofification", content);
						}
					}
				}
				catch (Exception e) {
					log.debug("failed sending notification", e);
				}
			}
		});
	}

	public void queueEmail(final String recipients, final String subject, final String content) throws Exception {
		adminRunner.run(new JcrRunnable() {
			@Override
			public void run(Session session) throws Exception {
				queueMailUsingAdminSession(session, recipients, subject, content);
			}
		});
	}

	public void queueMailUsingAdminSession(Session session, final String recipients, final String subject, final String content) throws Exception {
		Node outboxNode = getSystemOutbox(session);

		String name = JcrUtil.getGUID();
		Node newNode = outboxNode.addNode(name, JcrConstants.NT_UNSTRUCTURED);
		newNode.setProperty(JcrProp.EMAIL_CONTENT, content);
		newNode.setProperty(JcrProp.EMAIL_SUBJECT, subject);
		newNode.setProperty(JcrProp.EMAIL_RECIP, recipients);
		JcrUtil.timestampNewNode(session, newNode);
		session.save();
	}

	/*
	 * Loads only up to 10 emails at a time
	 * 
	 * TODO: will need to make this batch size configurable for scalability.
	 */
	public List<Node> getMailNodes(Session session) throws Exception {
		int mailBatchSize = 10;
		List<Node> mailNodes = null;

		Node outboxNode = getSystemOutbox(session);
		NodeIterator nodeIter = outboxNode.getNodes();
		try {
			int nodeCount = 0;
			while (nodeCount++ < mailBatchSize) {
				Node n = nodeIter.nextNode();

				if (mailNodes == null) {
					mailNodes = new LinkedList<Node>();
				}
				mailNodes.add(n);
			}
		}
		catch (NoSuchElementException ex) {
			// not an error. Normal iterator end condition.
		}
		return mailNodes;
	}

	/*
	 * Get node that contains all preferences for this user, as properties on it.
	 */
	public static Node getSystemOutbox(Session session) throws Exception {
		return JcrUtil.ensureNodeExists(session, "/" + JcrName.OUTBOX + "/", JcrName.SYSTEM, "System Messages");
	}
}
