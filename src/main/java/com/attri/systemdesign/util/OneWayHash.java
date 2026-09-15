package com.attri.systemdesign.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Stateless SHA-256 hashing utility. */
public final class OneWayHash {

    private static final String ALGORITHM = "SHA-256";
    private static final String BASE_62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final BigInteger BASE_62 = BigInteger.valueOf(BASE_62_ALPHABET.length());
    private static final int SHA_256_BASE_62_LENGTH = 43;
    private static final JsonMapper CANONICAL_JSON = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    private OneWayHash() {
    }

    /** Returns the lower-case hexadecimal SHA-256 digest of UTF-8 text. */
    public static String hash(String input) {
        return hash(Objects.requireNonNull(input, "input must not be null").getBytes(StandardCharsets.UTF_8));
    }

    /** Returns the lower-case hexadecimal SHA-256 digest of the supplied bytes. */
    public static String hash(byte[] input) {
        return HexFormat.of().formatHex(digest(input));
    }

    /** Returns a fixed-width, alphanumeric Base62 representation of a SHA-256 hash. */
    public static String hashBase62(String input) {
        return hashBase62(Objects.requireNonNull(input, "input must not be null").getBytes(StandardCharsets.UTF_8));
    }

    /** Returns a fixed-width, alphanumeric Base62 representation of a SHA-256 hash. */
    public static String hashBase62(byte[] input) {
        return toBase62(digest(input));
    }

    /**
     * Hashes an object after deterministic JSON serialization. Object property
     * names and map keys are sorted, making logically equivalent map content
     * hash identically regardless of insertion order.
     */
    public static String hash(Object value) {
        Objects.requireNonNull(value, "value must not be null");
        if (value instanceof String string) {
            return hash(string);
        }
        if (value instanceof byte[] bytes) {
            return hash(bytes);
        }
        try {
            return hash(CANONICAL_JSON.writeValueAsBytes(value));
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("value cannot be serialized for hashing", exception);
        }
    }

    /** Hashes a deterministically serialized object and returns its Base62 digest. */
    public static String hashBase62(Object value) {
        Objects.requireNonNull(value, "value must not be null");
        if (value instanceof String string) {
            return hashBase62(string);
        }
        if (value instanceof byte[] bytes) {
            return hashBase62(bytes);
        }
        try {
            return hashBase62(CANONICAL_JSON.writeValueAsBytes(value));
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("value cannot be serialized for hashing", exception);
        }
    }

    private static byte[] digest(byte[] input) {
        Objects.requireNonNull(input, "input must not be null");
        try {
            return MessageDigest.getInstance(ALGORITHM).digest(input);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(ALGORITHM + " is not available", exception);
        }
    }

    private static String toBase62(byte[] digest) {
        BigInteger remaining = new BigInteger(1, digest);
        StringBuilder encoded = new StringBuilder(SHA_256_BASE_62_LENGTH);
        do {
            BigInteger[] quotientAndRemainder = remaining.divideAndRemainder(BASE_62);
            encoded.append(BASE_62_ALPHABET.charAt(quotientAndRemainder[1].intValue()));
            remaining = quotientAndRemainder[0];
        } while (remaining.signum() > 0);

        return "0".repeat(SHA_256_BASE_62_LENGTH - encoded.length()) + encoded.reverse();
    }
}
