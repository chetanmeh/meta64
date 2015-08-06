package com.meta64.mobile.user;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.meta64.mobile.service.UserManagerService;
import com.meta64.mobile.util.JcrRunnable;

@Component
@Scope("singleton")
public class UserSettingsDaemon {

	private static final Logger log = LoggerFactory.getLogger(UserSettingsDaemon.class);

	@Autowired
	private UserManagerService userManagerService;

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
						session.save();
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
		Iterator it = mapByUser.entrySet().iterator();
		
		/* One iteration here per user */
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			String userName = (String) pair.getKey();
			saveSettingsForUser(session, userName, (UnsavedUserSettings) pair.getValue());
		}
	}

	private void saveSettingsForUser(Session session, String userName, UnsavedUserSettings settings) throws Exception {
		if ("anonymous".equals(userName.toLowerCase())) {
			return;
		}

		Node prefsNode = userManagerService.getPrefsNodeForSessionUser(session, userName);

		/* one iteration here per unsaved property */
		Iterator it = settings.getMap().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			Object val = pair.getValue();
			if (val != null) {
				//log.debug(pair.getKey() + " = " + pair.getValue());
				prefsNode.setProperty((String) pair.getKey(), (String) pair.getValue());
			}
		}
		settings.getMap().clear();
	}

	public void setSettingVal(String userName, String propertyName, Object propertyVal) {
		UnsavedUserSettings settings = mapByUser.get(userName);
		if (settings == null) {
			settings = new UnsavedUserSettings();
			mapByUser.put(userName, settings);
		}
		settings.getMap().put(propertyName, propertyVal);
		settingsDirty = true;
	}
}
