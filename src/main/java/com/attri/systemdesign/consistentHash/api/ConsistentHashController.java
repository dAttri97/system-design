package com.attri.systemdesign.consistenthash.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.attri.systemdesign.consistenthash.domain.NodeId;
import com.attri.systemdesign.consistenthash.ring.ConsistentHashRing;
import com.attri.systemdesign.consistenthash.ring.RingDistributionAnalyzer;
import com.attri.systemdesign.consistenthash.ring.RingDistributionStats;

@RestController
@RequestMapping("/api/consistent-hash")
@ConditionalOnProperty(prefix = "consistent-hash", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConsistentHashController {

	private final ConsistentHashRing ring;

	public ConsistentHashController(ConsistentHashRing ring) {
		this.ring = ring;
	}

	@GetMapping("/locate")
	public ResponseEntity<Map<String, Object>> locate(@RequestParam String key) {
		return ring.locate(key)
				.map(node -> ResponseEntity.ok(Map.<String, Object>of(
						"key", key,
						"nodeId", node.value(),
						"ringVersion", ring.ringVersion())))
				.orElse(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
						.body(Map.of("key", key, "error", "ring_empty")));
	}

	@GetMapping("/nodes")
	public ResponseEntity<Map<String, Object>> nodes() {
		return ResponseEntity.ok(Map.of(
				"nodes", ring.physicalNodes().stream().map(NodeId::value).sorted().toList(),
				"virtualNodesPerServer", ring.virtualNodesPerPhysicalNode(),
				"vnodeCount", ring.vnodeCount(),
				"ringVersion", ring.ringVersion()));
	}

	@PostMapping("/nodes/{nodeId}")
	public ResponseEntity<Map<String, Object>> addNode(@PathVariable String nodeId) {
		long versionBefore = ring.ringVersion();
		ring.addNode(new NodeId(nodeId));
		return ResponseEntity.ok(Map.of(
				"nodeId", nodeId,
				"ringVersionBefore", versionBefore,
				"ringVersion", ring.ringVersion(),
				"vnodeCount", ring.vnodeCount()));
	}

	@DeleteMapping("/nodes/{nodeId}")
	public ResponseEntity<Map<String, Object>> removeNode(@PathVariable String nodeId) {
		long versionBefore = ring.ringVersion();
		boolean removed = ring.removeNode(new NodeId(nodeId));
		if (!removed) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("nodeId", nodeId, "error", "not_found"));
		}
		return ResponseEntity.ok(Map.of(
				"nodeId", nodeId,
				"ringVersionBefore", versionBefore,
				"ringVersion", ring.ringVersion(),
				"vnodeCount", ring.vnodeCount()));
	}

	@GetMapping("/stats")
	public ResponseEntity<Map<String, Object>> stats(
			@RequestParam(defaultValue = "10000") int sampleSize) {
		if (ring.physicalNodes().isEmpty()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body(Map.of("error", "ring_empty"));
		}
		RingDistributionStats stats = RingDistributionAnalyzer.analyze(ring, sampleSize);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("sampleSize", stats.sampleSize());
		body.put("meanKeysPerNode", stats.meanKeysPerNode());
		body.put("standardDeviation", stats.standardDeviation());
		body.put("coefficientOfVariation", stats.coefficientOfVariation());
		body.put("keysPerNode", stats.keysPerNode().entrySet().stream()
				.collect(Collectors.toMap(entry -> entry.getKey().value(), Map.Entry::getValue)));
		body.put("ringVersion", ring.ringVersion());
		return ResponseEntity.ok(body);
	}
}
