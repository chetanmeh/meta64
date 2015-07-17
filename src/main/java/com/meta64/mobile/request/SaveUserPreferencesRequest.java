package com.meta64.mobile.request;

import com.meta64.mobile.model.UserPreferences;

public class SaveUserPreferencesRequest {
	private UserPreferences userPreferences;

	public UserPreferences getUserPreferences() {
		return userPreferences;
	}

	public void setUserPreferences(UserPreferences userPreferences) {
		this.userPreferences = userPreferences;
	}
}
