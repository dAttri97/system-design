package com.attri.systemdesign.bitly.util;

public final class Base62Encoder {

	private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final int BASE = ALPHABET.length();

	private Base62Encoder() {
	}

	public static String encode(long value) {
		if (value == 0) {
			return "0";
		}
		if (value < 0) {
			throw new IllegalArgumentException("value must be non-negative");
		}

		StringBuilder encoded = new StringBuilder();
		long remaining = value;
		while (remaining > 0) {
			int index = (int) (remaining % BASE);
			encoded.append(ALPHABET.charAt(index));
			remaining /= BASE;
		}
		return encoded.reverse().toString();
	}
}
