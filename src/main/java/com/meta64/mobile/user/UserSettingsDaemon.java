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
 * This would best be described as a "Background Thread" which saves user settings for all users (who are logged in)
 * 
 * TODO: need to use a listener for when a session becomes invalid for whatever reason (logout, timeout,...), and remove
 * that entry from the global 'mapByUser' map, just to keep from having a memory leak. Right now it leaks, but only leaks
 * a few bites per user login, so unless the site is massively going viral this is not a problem yet, and no one Cluster node would
 * ever actually host enough users for it to be a significant leak. But all leaks are bad and I'll fix this.
 */
@Component
@Scope("singleton")
public class UserSettingsDaemon {

	private static final Logger log = LoggerFactory.getLogger(UserSettingsDaemon.class);

	@Autowired
	private RunAsJcrAdmin adminRunner;

	/*
	 * Settings dirty should be thought of as a performance 'hint' rather than a failsafe locking
	 * mechanism that controls any program flow. We allow mayByUser to be modified EVEN while we are
	 * processing it, because we consider performance better than blocking here. This is just
	 * persisting user preferences and should not be allowed to have impact on performance a lock
	 * would incur.
	 * 
	 * Dirty means there are unsaved changes.
	 */
	private boolean settingsDirty = false;

	private final HashMap<String, UnsavedUserSettings> mapByUser = new HashMap<String, UnsavedUserSettings>();

	@Scheduled(fixedDelay = 20 * 1000)
	public void run() {
		if (!settingsDirty) return;

		try {
			adminRunner.run(new JcrRunnable() {
				@Override
				public void run(Session session) throws Exception {
					try {
						saveSettings(session);
						settingsDirty = false;
					}
					catch (Exception e) {
						log.debug("Failed saving settings.");
					}
				}
			});
		}
		catch (Exception e) {
			log.debug("Failed processing mail.", e);
		}
	}

	private void saveSettings(Session session) throws Exception {
		for (Map.Entry<String, UnsavedUserSettings> entry : mapByUser.entrySet()) {
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

	public void setSettingVal(String userName, String propertyName, Object propertyVal) {
		if (userName == null) {
			return;
		}
		UnsavedUserSettings settings = mapByUser.get(userName);
		if (settings == null) {
			settings = new UnsavedUserSettings();
			mapByUser.put(userName, settings);
		}
		settings.getMap().put(propertyName, propertyVal);
		settingsDirty = true;
	}
}
