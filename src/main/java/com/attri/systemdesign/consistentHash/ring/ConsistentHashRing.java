package com.attri.systemdesign.consistenthash.ring;

import java.util.Optional;
import java.util.Set;

import com.attri.systemdesign.consistenthash.domain.NodeId;

/**
 * Maps keys to physical nodes via consistent hashing on a ring with virtual nodes.
 */
public interface ConsistentHashRing {

	Optional<NodeId> locate(String key);

	Optional<NodeId> locate(byte[] key);

	void addNode(NodeId nodeId);

	boolean removeNode(NodeId nodeId);

	Set<NodeId> physicalNodes();

	int virtualNodesPerPhysicalNode();

	long ringVersion();

	int vnodeCount();
}
