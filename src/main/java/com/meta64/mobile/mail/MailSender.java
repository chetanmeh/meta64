package com.meta64.mobile.mail;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.event.TransportEvent;
import javax.mail.event.TransportListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/*
 * Implements and processes the sending of emails.
 */
@Component
@Scope("prototype")
public class MailSender implements TransportListener {

	private static final Logger log = LoggerFactory.getLogger(MailSender.class);

	@Value("${mail.host}")
	public String mailHost;

	@Value("${mail.port}")
	public String mailPort;

	@Value("${mail.user}")
	public String mailUser;

	@Value("${mail.password}")
	public String mailPassword;

	public static final String MIME_HTML = "text/html";
	public int TIMEOUT = 10000; // ten seconds
	public int TIMESLICE = 250; // quarter second

	public boolean debug = true;
	public boolean success = false;
	public boolean waiting = false;

	private Properties props;
	private Session mailSession;
	private Transport transport;

	public void init() throws Exception {

		log.debug("Creating mail sender.");

		props = new Properties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.host", mailHost);

		/*
		 * how did I end up with 'put' instead of 'setProperty' here? Cut-n-paste from somewhere
		 */
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", mailPort);

		props.put("mail.user", mailUser);
		props.put("mail.password", mailPassword);

		mailSession = Session.getDefaultInstance(props, null);
		mailSession.setDebug(debug);

		transport = mailSession.getTransport("smtp");
		transport.addTransportListener(this);
		transport.connect(mailHost, mailUser, mailPassword);
	}

	public boolean isBusy() {
		return waiting;
	}

	public boolean sendMail(String sendToAddress, String content, String subjectLine) throws Exception {

		if (transport == null) {
			throw new Exception("Tried to use MailSender after close() call or without initializing.");
		}

		if (waiting) {
			throw new Exception("concurrency must be done via 'isBusy' before each call");
		}

		log.debug("send to address [" + sendToAddress + "]");

		MimeMessage message = new MimeMessage(mailSession);
		message.setSentDate(new Date());
		message.setSubject(subjectLine);
		message.setFrom(new InternetAddress(mailUser));
		message.setRecipient(Message.RecipientType.TO, new InternetAddress(sendToAddress));

		// MULTIPART
		// ---------------
		// MimeMultipart multipart = new MimeMultipart("part");
		// BodyPart messageBodyPart = new MimeBodyPart();
		// messageBodyPart.setContent(content, "text/html");
		// multipart.addBodyPart(messageBodyPart);
		// message.setContent(multipart);

		// SIMPLE (no multipart)
		// ---------------
		message.setContent(content, MIME_HTML);

		// can get alreadyconnected exception here ??
		// transport.connect(mailHost, mailUser, mailPassword);

		success = false;

		/*
		 * important: while inside this 'sendMessage' method, the 'messageDelivered' callback will
		 * get called if the send is successful, so we can return the value below, even though we do
		 * not set it in this method
		 */
		int timeRemaining = TIMEOUT;
		waiting = true;
		transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
		while (waiting && timeRemaining > 0) {
			Thread.sleep(TIMESLICE);
			timeRemaining -= TIMESLICE;
		}

		/* if we are still pending, that means a timeout so we give up */
		if (waiting) {
			waiting = false;
			log.debug("mail send failed.");
			throw new Exception("mail system is not responding.  Email send failed.");
		}

		return success;
	}

	public void close() throws Exception {
		if (transport != null) {
			transport.close();
			transport = null;
		}
	}

	@Override
	public void messageDelivered(TransportEvent arg) {
		success = true;
		waiting = false;
	}

	@Override
	public void messageNotDelivered(TransportEvent arg) {
		success = false;
		waiting = false;
	}

	@Override
	public void messagePartiallyDelivered(TransportEvent arg) {
		success = false;
		waiting = false;
	}
}
