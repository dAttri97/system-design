package com.attri.systemdesign.bitly.exception;

public class AliasConflictException extends RuntimeException {

	public AliasConflictException(String alias) {
		super("Alias unavailable: " + alias);
	}
}
