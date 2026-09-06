package com.attri.systemdesign.bitly.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UrlValidatorTest {

	@Test
	void acceptsHttpAndHttpsUrls() {
		assertTrue(UrlValidator.isValidLongUrl("https://www.example.com/path"));
		assertTrue(UrlValidator.isValidLongUrl("http://example.com"));
	}

	@Test
	void rejectsInvalidUrls() {
		assertFalse(UrlValidator.isValidLongUrl(""));
		assertFalse(UrlValidator.isValidLongUrl("ftp://example.com"));
		assertFalse(UrlValidator.isValidLongUrl("not-a-url"));
	}

	@Test
	void validatesCustomAliasPattern() {
		assertTrue(UrlValidator.isValidCustomAlias("my-launch"));
		assertFalse(UrlValidator.isValidCustomAlias("ab"));
		assertFalse(UrlValidator.isValidCustomAlias("bad alias"));
	}
}
