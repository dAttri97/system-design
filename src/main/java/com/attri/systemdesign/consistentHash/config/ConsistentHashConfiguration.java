package com.attri.systemdesign.consistenthash.config;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.attri.systemdesign.consistenthash.domain.NodeId;
import com.attri.systemdesign.consistenthash.ring.ConsistentHashRing;
import com.attri.systemdesign.consistenthash.ring.DefaultConsistentHashRing;

@Configuration
@EnableConfigurationProperties(ConsistentHashProperties.class)
@ConditionalOnProperty(prefix = "consistent-hash", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConsistentHashConfiguration {

	@Bean
	public ConsistentHashRing consistentHashRing(
			com.attri.systemdesign.consistenthash.config.ConsistentHashProperties properties,
			ConsistentHashNodesLoader nodesLoader) throws IOException {
		DefaultConsistentHashRing ring = new DefaultConsistentHashRing(properties.getVirtualNodesPerServer());
		List<NodeId> bootstrapNodes = nodesLoader.load();
		for (NodeId nodeId : bootstrapNodes) {
			ring.addNode(nodeId);
		}
		return ring;
	}
}
