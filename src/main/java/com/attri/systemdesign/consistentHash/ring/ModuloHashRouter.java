package com.attri.systemdesign.consistenthash.ring;

import java.util.List;
import java.util.Objects;

import com.attri.systemdesign.consistenthash.domain.NodeId;
import com.attri.systemdesign.consistenthash.hash.RingHashFunction;

/**
 * Naive {@code hash(key) % N} routing for comparison tests (high remap rate on membership change).
 */
public final class ModuloHashRouter {

	private final RingHashFunction hashFunction;
	private final List<NodeId> nodes;

	public ModuloHashRouter(RingHashFunction hashFunction, List<NodeId> nodes) {
		this.hashFunction = Objects.requireNonNull(hashFunction, "hashFunction");
		this.nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
		if (this.nodes.isEmpty()) {
			throw new IllegalArgumentException("nodes must not be empty");
		}
	}

	public NodeId locate(String key) {
		long hash = hashFunction.hash(key);
		int index = (int) Long.remainderUnsigned(hash, nodes.size());
		return nodes.get(index);
	}
}
