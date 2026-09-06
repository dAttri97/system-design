package com.attri.systemdesign.bitly.exception;

public class UrlNotFoundException extends RuntimeException {

	public UrlNotFoundException(String shortCode) {
		super("Link not found: " + shortCode);
	}
}
