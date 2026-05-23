package com.attri.systemdesign.consistenthash.ring;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.attri.systemdesign.consistenthash.domain.NodeId;

/**
 * Samples synthetic keys against a ring to measure load skew across physical nodes.
 */
public final class RingDistributionAnalyzer {

	private RingDistributionAnalyzer() {
	}

	public static RingDistributionStats analyze(ConsistentHashRing ring, int sampleSize) {
		if (sampleSize < 1) {
			throw new IllegalArgumentException("sampleSize must be >= 1");
		}
		Set<NodeId> nodes = ring.physicalNodes();
		if (nodes.isEmpty()) {
			throw new IllegalStateException("ring has no nodes");
		}

		Map<NodeId, Integer> counts = new HashMap<>();
		for (NodeId node : nodes) {
			counts.put(node, 0);
		}

		for (int i = 0; i < sampleSize; i++) {
			String key = "sample-key-" + i;
			NodeId owner = ring.locate(key).orElseThrow();
			counts.merge(owner, 1, Integer::sum);
		}

		double mean = sampleSize / (double) nodes.size();
		double varianceSum = 0;
		for (int count : counts.values()) {
			double delta = count - mean;
			varianceSum += delta * delta;
		}
		double stdDev = Math.sqrt(varianceSum / nodes.size());
		double coefficientOfVariation = mean == 0 ? 0 : stdDev / mean;

		return new RingDistributionStats(sampleSize, Map.copyOf(counts), mean, stdDev, coefficientOfVariation);
	}
}
