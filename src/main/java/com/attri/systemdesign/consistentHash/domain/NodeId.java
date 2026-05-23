package com.attri.systemdesign.consistenthash.domain;

import java.util.Objects;

/**
 * Stable identifier for a physical server or shard on the hash ring.
 */
public record NodeId(String value) {

	public NodeId {
		Objects.requireNonNull(value, "value");
		if (value.isBlank()) {
			throw new IllegalArgumentException("node id must not be blank");
		}
	}

	@Override
	public String toString() {
		return value;
	}
}
