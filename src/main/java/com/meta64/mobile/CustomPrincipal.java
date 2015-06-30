package com.meta64.mobile;

import java.security.Principal;

/**
 * Represents a Principal of a given name.
 */
public class CustomPrincipal implements Principal {

	private String name;

	public CustomPrincipal(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}
}
