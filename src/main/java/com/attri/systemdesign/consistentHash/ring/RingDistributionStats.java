package com.attri.systemdesign.consistenthash.ring;

import java.util.Map;

import com.attri.systemdesign.consistenthash.domain.NodeId;

/**
 * Key counts per physical node from a sample workload.
 */
public record RingDistributionStats(
		int sampleSize,
		Map<NodeId, Integer> keysPerNode,
		double meanKeysPerNode,
		double standardDeviation,
		double coefficientOfVariation) {
}
