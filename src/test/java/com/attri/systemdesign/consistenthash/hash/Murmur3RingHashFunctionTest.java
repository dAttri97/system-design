package com.attri.systemdesign.consistenthash.hash;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Murmur3RingHashFunctionTest {

	private final Murmur3RingHashFunction hashFunction = new Murmur3RingHashFunction();

	@Test
	void hashIsDeterministic() {
		assertThat(hashFunction.hash("hello")).isEqualTo(hashFunction.hash("hello"));
		assertThat(hashFunction.hash("hello".getBytes())).isEqualTo(hashFunction.hash("hello".getBytes()));
	}

	@Test
	void differentInputsProduceDifferentHashes() {
		assertThat(hashFunction.hash("a")).isNotEqualTo(hashFunction.hash("b"));
	}
}
