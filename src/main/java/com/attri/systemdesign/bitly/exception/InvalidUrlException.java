package com.attri.systemdesign.bitly.exception;

public class InvalidUrlException extends RuntimeException {

	public InvalidUrlException(String message) {
		super(message);
	}
}
