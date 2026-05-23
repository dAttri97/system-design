package com.attri.systemdesign.consistenthash.hash;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 64-bit ring positions derived from SHA-256 (first eight bytes, big-endian).
 * Chosen for correctness and stable distribution; Murmur3 can replace this later if needed.
 */
public final class Murmur3RingHashFunction implements RingHashFunction {

	private static final String DIGEST = "SHA-256";

	@Override
	public long hash(String input) {
		return hash(input.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public long hash(byte[] input) {
		try {
			byte[] digest = MessageDigest.getInstance(DIGEST).digest(input);
			return ByteBuffer.wrap(digest).order(ByteOrder.BIG_ENDIAN).getLong();
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(DIGEST + " not available", exception);
		}
	}
}
