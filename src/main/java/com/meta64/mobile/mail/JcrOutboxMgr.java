package com.meta64.mobile.mail;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.JcrName;
import com.meta64.mobile.config.JcrProp;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.util.JcrRunnable;
import com.meta64.mobile.util.JcrUtil;

@Component
@Scope("singleton")
public class JcrOutboxMgr {

	@Autowired
	private OakRepositoryBean oak;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	public void queueEmail(final String recipients, final String subject, final String content) throws Exception {
		adminRunner.run(new JcrRunnable() {
			@Override
			public void run(Session session) throws Exception {
				Node outboxNode = getSystemOutbox(session);

				String name = JcrUtil.getGUID();
				Node newNode = outboxNode.addNode(name, JcrConstants.NT_UNSTRUCTURED);
				newNode.setProperty(JcrProp.EMAIL_CONTENT, content);
				newNode.setProperty(JcrProp.EMAIL_SUBJECT, subject);
				newNode.setProperty(JcrProp.EMAIL_RECIP, recipients);
				JcrUtil.timestampNewNode(session, newNode);
				session.save();
			}
		});
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
