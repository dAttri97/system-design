package com.attri.systemdesign.consistenthash.ring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.attri.systemdesign.consistenthash.domain.NodeId;
import com.attri.systemdesign.consistenthash.hash.Murmur3RingHashFunction;

class DefaultConsistentHashRingTest {

	private DefaultConsistentHashRing ring;

	@BeforeEach
	void setUp() {
		ring = new DefaultConsistentHashRing(150);
	}

	@Test
	void locateReturnsEmptyWhenRingIsEmpty() {
		assertThat(ring.locate("any-key")).isEmpty();
	}

	@Test
	void singleNodeOwnsAllKeys() {
		ring.addNode(new NodeId("only"));
		for (int i = 0; i < 100; i++) {
			assertThat(ring.locate("key-" + i)).contains(new NodeId("only"));
		}
	}

	@Test
	void addNodeIsIdempotent() {
		NodeId node = new NodeId("cache-01");
		ring.addNode(node);
		int vnodesAfterFirst = ring.vnodeCount();
		long versionAfterFirst = ring.ringVersion();
		ring.addNode(node);
		assertThat(ring.vnodeCount()).isEqualTo(vnodesAfterFirst);
		assertThat(ring.ringVersion()).isEqualTo(versionAfterFirst);
	}

	@Test
	void removeNodeReturnsFalseWhenMissing() {
		assertThat(ring.removeNode(new NodeId("missing"))).isFalse();
	}

	@Test
	void removeNodeClearsOwnership() {
		ring.addNode(new NodeId("a"));
		ring.addNode(new NodeId("b"));
		assertThat(ring.removeNode(new NodeId("a"))).isTrue();
		assertThat(ring.physicalNodes()).containsExactly(new NodeId("b"));
		assertThat(ring.locate("key-1")).contains(new NodeId("b"));
	}

	@Test
	void locateIsDeterministicForStableRing() {
		ring.addNode(new NodeId("n1"));
		ring.addNode(new NodeId("n2"));
		ring.addNode(new NodeId("n3"));
		Optional<NodeId> first = ring.locate("user-42");
		for (int i = 0; i < 50; i++) {
			assertThat(ring.locate("user-42")).isEqualTo(first);
		}
	}

	@Test
	void addingNodeRemapsRoughlyOneOverNKeys() {
		int nodeCount = 10;
		int keyCount = 10_000;
		for (int i = 0; i < nodeCount; i++) {
			ring.addNode(new NodeId("node-" + i));
		}

		Map<String, NodeId> before = mapKeys(keyCount);
		ring.addNode(new NodeId("node-new"));
		double remapRate = remapRate(before, mapKeys(keyCount));

		assertThat(remapRate).isBetween(0.03, 0.20);
	}

	@Test
	void moduloRouterRemapsMostKeysOnScaleOut() {
		int nodeCount = 10;
		int keyCount = 10_000;
		List<NodeId> nodes = java.util.stream.IntStream.range(0, nodeCount)
				.mapToObj(i -> new NodeId("node-" + i))
				.toList();
		var hash = new Murmur3RingHashFunction();
		ModuloHashRouter router = new ModuloHashRouter(hash, nodes);

		Map<String, NodeId> before = new HashMap<>();
		for (int i = 0; i < keyCount; i++) {
			String key = "key-" + i;
			before.put(key, router.locate(key));
		}

		List<NodeId> expanded = java.util.stream.IntStream.range(0, nodeCount + 1)
				.mapToObj(i -> new NodeId("node-" + i))
				.toList();
		ModuloHashRouter expandedRouter = new ModuloHashRouter(hash, expanded);

		double remapRate = 0;
		for (int i = 0; i < keyCount; i++) {
			String key = "key-" + i;
			if (!before.get(key).equals(expandedRouter.locate(key))) {
				remapRate++;
			}
		}
		remapRate /= keyCount;

		assertThat(remapRate).isGreaterThan(0.5);
	}

	@Test
	void distributionSkewIsWithinReasonableBounds() {
		ring.addNode(new NodeId("a"));
		ring.addNode(new NodeId("b"));
		ring.addNode(new NodeId("c"));

		RingDistributionStats stats = RingDistributionAnalyzer.analyze(ring, 20_000);
		double relativeStdDev = stats.standardDeviation() / stats.meanKeysPerNode();

		assertThat(relativeStdDev).isLessThan(0.12);
	}

	@Test
	void concurrentLookupsRemainConsistent() throws InterruptedException {
		for (int i = 0; i < 5; i++) {
			ring.addNode(new NodeId("node-" + i));
		}
		Optional<NodeId> expected = ring.locate("hot-key");

		int threads = 16;
		int iterations = 5_000;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		AtomicInteger mismatches = new AtomicInteger();

		for (int t = 0; t < threads; t++) {
			executor.submit(() -> {
				try {
					start.await();
					for (int i = 0; i < iterations; i++) {
						if (!ring.locate("hot-key").equals(expected)) {
							mismatches.incrementAndGet();
						}
					}
				}
				catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
				}
				finally {
					done.countDown();
				}
			});
		}

		start.countDown();
		done.await();
		executor.shutdownNow();

		assertThat(mismatches).hasValue(0);
	}

	@Test
	void rejectsBlankNodeId() {
		assertThatThrownBy(() -> new NodeId("  ")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void ringVersionIncrementsOnMembershipChange() {
		assertThat(ring.ringVersion()).isZero();
		ring.addNode(new NodeId("a"));
		assertThat(ring.ringVersion()).isEqualTo(1);
		ring.addNode(new NodeId("b"));
		assertThat(ring.ringVersion()).isEqualTo(2);
		ring.removeNode(new NodeId("a"));
		assertThat(ring.ringVersion()).isEqualTo(3);
	}

	@Test
	void vnodeCountMatchesPhysicalNodesTimesReplicas() {
		ring.addNode(new NodeId("x"));
		ring.addNode(new NodeId("y"));
		assertThat(ring.vnodeCount()).isEqualTo(300);
		assertThat(ring.physicalNodes()).isEqualTo(Set.of(new NodeId("x"), new NodeId("y")));
	}

	private Map<String, NodeId> mapKeys(int keyCount) {
		Map<String, NodeId> mapping = new HashMap<>();
		for (int i = 0; i < keyCount; i++) {
			String key = "key-" + i;
			mapping.put(key, ring.locate(key).orElseThrow());
		}
		return mapping;
	}

	private static double remapRate(Map<String, NodeId> before, Map<String, NodeId> after) {
		int remapped = 0;
		for (Map.Entry<String, NodeId> entry : before.entrySet()) {
			if (!entry.getValue().equals(after.get(entry.getKey()))) {
				remapped++;
			}
		}
		return remapped / (double) before.size();
	}
}
