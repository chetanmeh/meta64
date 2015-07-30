package com.meta64.mobile.mail;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.JcrProp;
import com.meta64.mobile.config.SpringContextUtil;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.util.JcrRunnable;
import com.meta64.mobile.util.JcrUtil;
import com.meta64.mobile.util.XString;

/* we need this daemon so that we can do email sends in a separate thread and not block any of the responsiveness 
 * of the gui 
 * 
 * Note: Will this demon still work acceptably even when there may be multiple ones running on different
 * instances of WARs ?  I think 'sticky sessions' is enough to make it work acceptably for now, because the editing
 * of a new node should happen on the same WAR where the node is created, and therefore the EmailNotification is created on
 * that same WAR.
 * 
 */
@Component
@Scope("singleton")
public class NotificationDaemon {

	private static final Logger log = LoggerFactory.getLogger(NotificationDaemon.class);

	@Autowired
	private RunAsJcrAdmin adminRunner;

	@Autowired
	private JcrOutboxMgr outboxMgr;

	@Value("${mail.host}")
	public String mailHost;

	private int runCounter = 0;

	/*
	 * Runs every 10 seconds. Note: Spring does correctly protect against concurrent runs. It will
	 * always wait until the last run of this function is completed before running again. So we can
	 * always assume only one thread/deamon of this class is running at at time, because this is a
	 * singleton class.
	 * 
	 * see also: @EnableScheduling (in this project)
	 */
	@Scheduled(fixedDelay = 60 * 1000)
	public void run() {

		/* spring always calls immediately upon startup and we will ignore the first call */
		if (runCounter++ == 0) {
			return;
		}

		/* fail fast if no mail host is configured. */
		if (XString.isEmpty(mailHost)) {
			if (runCounter < 3) {
				log.debug("NotificationDaemon is disabled, because no mail server is configured.");
			}
			return;
		}

		try {
			adminRunner.run(new JcrRunnable() {

				@Override
				public void run(Session session) throws Exception {

					List<Node> mailNodes = outboxMgr.getMailNodes(session);
					if (mailNodes != null) {
						sendAllMail(session, mailNodes);
					}
				}
			});
		}
		catch (Exception e) {
			log.debug("Failed processing mail.", e);
		}
	}

	private void sendAllMail(Session session, List<Node> nodes) throws Exception {

		MailSender mailSender = null;

		try {
			mailSender = SpringContextUtil.getApplicationContext().getBean(MailSender.class);
			mailSender.init();

			for (Node node : nodes) {

				String email = JcrUtil.getRequiredStringProp(node, JcrProp.EMAIL_RECIP);
				String subject = JcrUtil.getRequiredStringProp(node, JcrProp.EMAIL_SUBJECT);
				String content = JcrUtil.getRequiredStringProp(node, JcrProp.EMAIL_CONTENT);

				if (mailSender.sendMail(email, content, subject)) {
					node.remove();
					session.save();
				}
			}
		}
		finally {
			if (mailSender != null) {
				try {
					log.debug("Closing mail sender after sending some mail(s).");
					mailSender.close();
					mailSender = null;
				}
				catch (Exception e) {
					log.debug("Failed closing mail sender object.", e);
					/* DO NOT rethrow. Don't want to blow up the daemon thread */
				}
			}
		}
	}
}
