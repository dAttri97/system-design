package com.attri.systemdesign.bitly.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public final class UrlValidator {

	private static final Pattern CUSTOM_ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,50}$");

	private UrlValidator() {
	}

	public static boolean isValidLongUrl(String url) {
		if (url == null || url.isBlank()) {
			return false;
		}
		try {
			URI uri = new URI(url.trim());
			String scheme = uri.getScheme();
			if (scheme == null) {
				return false;
			}
			return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
		}
		catch (URISyntaxException ex) {
			return false;
		}
	}

	public static boolean isValidCustomAlias(String alias) {
		return alias != null && CUSTOM_ALIAS_PATTERN.matcher(alias).matches();
	}

	public static boolean isValidShortCode(String shortCode) {
		return shortCode != null && CUSTOM_ALIAS_PATTERN.matcher(shortCode).matches();
	}
}
