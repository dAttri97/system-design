package com.attri.systemdesign.consistenthash.hash;

/**
 * Maps keys and node identities to positions on the hash ring.
 */
public interface RingHashFunction {

	long hash(String input);

	long hash(byte[] input);
}
