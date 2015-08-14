package com.meta64.mobile.user;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.JcrPrincipal;
import com.meta64.mobile.service.UserManagerService;
import com.meta64.mobile.util.JcrRunnable;

/*
 * This is a "Background Thread" which saves user settings for all users (who are logged in)
 */
@Component
@Scope("singleton")
public class UserSettingsDaemon {

	private static final Logger log = LoggerFactory.getLogger(UserSettingsDaemon.class);

	@Autowired
	private RunAsJcrAdmin adminRunner;

	private Object lock = new Object();
	private HashMap<String, UnsavedUserSettings> mapByUser = null;

	@Scheduled(fixedDelay = 20 * 1000)
	public void run() {
		log.trace("UserSettingsDeamon.run");
		synchronized (lock) {
			if (mapByUser == null) {
				log.trace("nothing to save.");
				return;
			}
		}
		log.trace("Saving settings.");

		try {
			adminRunner.run(new JcrRunnable() {
				@Override
				public void run(Session session) throws Exception {
					saveSettings(session);
				}
			});
		}
		catch (Exception e) {
			log.debug("Failed saving settings.", e);
		}
	}

	private void saveSettings(Session session) throws Exception {

		/*
		 * In order to not hold the lock for more than on the order of a millisecond we use a local
		 * variable to hold a reference to the mapByUser data, and then immediately release the lock
		 * so other web servicing threads will never wait long for this lock, for any significant
		 * time. Importantly we set the mapByUser member back to null so that it will be capturing
		 * new incomming data in a new collection. In this way we can avoid doing a deep clone AND
		 * have the 'laziest' loading and cleanest memory possible for this algorithm.
		 */
		HashMap<String, UnsavedUserSettings> mapByUserLocal = null;
		synchronized (lock) {
			mapByUserLocal = mapByUser;

			/*
			 * now we clear out the mapByUser so that it will be able to be collecting new data that
			 * comes in concurrently with the save operation that is being done from mapByUserClone
			 */
			mapByUser = null;
		}

		/*
		 * Now that we are operating with our own private clone local to this scope it doesn't
		 * matter how long it takes to persist all to the DB. This thread is a background thread and
		 * can never block any other system operations other than actual locks in the DB layer
		 * itself.
		 */
		for (Map.Entry<String, UnsavedUserSettings> entry : mapByUserLocal.entrySet()) {
			saveSettingsForUser(session, entry.getKey(), entry.getValue());
		}
		session.save();
	}

	private void saveSettingsForUser(Session session, String userName, UnsavedUserSettings settings) throws Exception {
		if (userName == null || JcrPrincipal.ANONYMOUS.equalsIgnoreCase(userName)) {
			return;
		}

		Node prefsNode = UserManagerService.getPrefsNodeForSessionUser(session, userName);

		/* one iteration here per unsaved property */
		for (Map.Entry<String, Object> entry : settings.getMap().entrySet()) {
			if (entry.getValue() != null) {
				// log.debug(entry.getKey() + " = " + entry.getValue());
				prefsNode.setProperty(entry.getKey(), (String) entry.getValue());
			}
		}
		settings.getMap().clear();
	}

	/*
	 * This method can get called by lots of different threads concurrently. These will be normal
	 * Web request servicing threads.
	 */
	public void setSettingVal(String userName, String propertyName, Object propertyVal) {

		if (userName == null) {
			return;
		}
		log.trace("Settings change: userName=" + userName + " prop: " + propertyName + "=" + propertyVal);

		UnsavedUserSettings settings = null;
		/*
		 * This lock obtain and release will be fast because no persistence is being done here, so
		 * as long as the other place that obtains this lock is only a short hold and is not
		 * wrapping any IO/persistence we will be fully scalable.
		 */
		synchronized (lock) {
			/* lazy create a mapByUser, at the last possible moment here */
			if (mapByUser == null) {
				mapByUser = new HashMap<String, UnsavedUserSettings>();
			}
			else {
				settings = mapByUser.get(userName);
			}

			if (settings == null) {
				settings = new UnsavedUserSettings();
				mapByUser.put(userName, settings);
			}
		}
		settings.getMap().put(propertyName, propertyVal);
		log.trace("Settings dirty = true");
	}
}
