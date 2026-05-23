package com.attri.systemdesign.consistenthash.ring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.attri.systemdesign.consistenthash.domain.NodeId;
import com.attri.systemdesign.consistenthash.hash.Murmur3RingHashFunction;
import com.attri.systemdesign.consistenthash.hash.RingHashFunction;

/**
 * Thread-safe consistent hash ring with clockwise successor lookup and virtual nodes.
 */
public final class DefaultConsistentHashRing implements ConsistentHashRing {

	public static final int DEFAULT_VIRTUAL_NODES = 150;

	private final RingHashFunction hashFunction;
	private final int virtualNodesPerPhysicalNode;
	private final NavigableMap<Long, NodeId> ring = new TreeMap<>(Long::compareUnsigned);
	private final Map<NodeId, List<Long>> positionsByNode = new java.util.HashMap<>();
	private final Set<NodeId> physicalNodes = new java.util.HashSet<>();
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private long ringVersion;

	public DefaultConsistentHashRing() {
		this(new Murmur3RingHashFunction(), DEFAULT_VIRTUAL_NODES);
	}

	public DefaultConsistentHashRing(RingHashFunction hashFunction, int virtualNodesPerPhysicalNode) {
		this.hashFunction = Objects.requireNonNull(hashFunction, "hashFunction");
		if (virtualNodesPerPhysicalNode < 1) {
			throw new IllegalArgumentException("virtualNodesPerPhysicalNode must be >= 1");
		}
		this.virtualNodesPerPhysicalNode = virtualNodesPerPhysicalNode;
	}

	public DefaultConsistentHashRing(int virtualNodesPerPhysicalNode) {
		this(new Murmur3RingHashFunction(), virtualNodesPerPhysicalNode);
	}

	@Override
	public Optional<NodeId> locate(String key) {
		Objects.requireNonNull(key, "key");
		return locateHash(hashFunction.hash(key));
	}

	@Override
	public Optional<NodeId> locate(byte[] key) {
		Objects.requireNonNull(key, "key");
		return locateHash(hashFunction.hash(key));
	}

	@Override
	public void addNode(NodeId nodeId) {
		Objects.requireNonNull(nodeId, "nodeId");
		lock.writeLock().lock();
		try {
			if (physicalNodes.contains(nodeId)) {
				return;
			}
			List<Long> positions = new ArrayList<>(virtualNodesPerPhysicalNode);
			for (int replica = 0; replica < virtualNodesPerPhysicalNode; replica++) {
				long position = placeVirtualNode(nodeId, replica);
				positions.add(position);
			}
			positionsByNode.put(nodeId, positions);
			physicalNodes.add(nodeId);
			ringVersion++;
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public boolean removeNode(NodeId nodeId) {
		Objects.requireNonNull(nodeId, "nodeId");
		lock.writeLock().lock();
		try {
			List<Long> positions = positionsByNode.remove(nodeId);
			if (positions == null) {
				return false;
			}
			for (Long position : positions) {
				ring.remove(position);
			}
			physicalNodes.remove(nodeId);
			ringVersion++;
			return true;
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public Set<NodeId> physicalNodes() {
		lock.readLock().lock();
		try {
			return Set.copyOf(physicalNodes);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public int virtualNodesPerPhysicalNode() {
		return virtualNodesPerPhysicalNode;
	}

	@Override
	public long ringVersion() {
		lock.readLock().lock();
		try {
			return ringVersion;
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public int vnodeCount() {
		lock.readLock().lock();
		try {
			return ring.size();
		}
		finally {
			lock.readLock().unlock();
		}
	}

	private Optional<NodeId> locateHash(long keyHash) {
		lock.readLock().lock();
		try {
			if (ring.isEmpty()) {
				return Optional.empty();
			}
			Map.Entry<Long, NodeId> successor = ring.ceilingEntry(keyHash);
			if (successor == null) {
				successor = ring.firstEntry();
			}
			return Optional.of(successor.getValue());
		}
		finally {
			lock.readLock().unlock();
		}
	}

	private long placeVirtualNode(NodeId nodeId, int replicaIndex) {
		int attempt = 0;
		while (true) {
			long position = hashVirtualNode(nodeId, replicaIndex, attempt);
			if (!ring.containsKey(position)) {
				ring.put(position, nodeId);
				return position;
			}
			attempt++;
		}
	}

	private long hashVirtualNode(NodeId nodeId, int replicaIndex, int collisionAttempt) {
		if (collisionAttempt == 0) {
			return hashFunction.hash(nodeId.value() + "#" + replicaIndex);
		}
		return hashFunction.hash(nodeId.value() + "#" + replicaIndex + "#" + collisionAttempt);
	}
}
