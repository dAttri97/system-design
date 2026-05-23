package com.attri.systemdesign.consistenthash.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.attri.systemdesign.consistenthash.domain.NodeId;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

@Component
public class ConsistentHashNodesLoader {

	private final ConsistentHashProperties properties;
	private final ResourceLoader resourceLoader;
	private final ObjectMapper yamlMapper = YAMLMapper.builder().build();

	public ConsistentHashNodesLoader(ConsistentHashProperties properties, ResourceLoader resourceLoader) {
		this.properties = properties;
		this.resourceLoader = resourceLoader;
	}

	public List<NodeId> load() throws IOException {
		String location = properties.getNodes().getPath();
		try (InputStream inputStream = openStream(location)) {
			ConsistentHashNodesDocument document = yamlMapper.readValue(inputStream, ConsistentHashNodesDocument.class);
			return document.getNodes().stream()
					.map(ConsistentHashNodesDocument.NodeEntry::getId)
					.map(NodeId::new)
					.toList();
		}
	}

	private InputStream openStream(String location) throws IOException {
		if (location.startsWith("classpath:")) {
			Resource resource = resourceLoader.getResource(location);
			if (!resource.exists()) {
				throw new IOException("Nodes resource not found: " + location);
			}
			return resource.getInputStream();
		}
		return Files.newInputStream(Path.of(location));
	}
}
