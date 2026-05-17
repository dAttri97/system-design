package com.attri.systemdesign.ratelimiter.rule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.attri.systemdesign.ratelimiter.config.RateLimiterProperties;
import com.attri.systemdesign.ratelimiter.domain.RateLimitRule;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

@Component
public class RateLimitRuleLoader {

	private final RateLimiterProperties properties;
	private final ResourceLoader resourceLoader;
	private final ObjectMapper yamlMapper = YAMLMapper.builder().build();

	public RateLimitRuleLoader(RateLimiterProperties properties, ResourceLoader resourceLoader) {
		this.properties = properties;
		this.resourceLoader = resourceLoader;
	}

	public List<RateLimitRule> load() throws IOException {
		String location = properties.getRules().getPath();
		try (InputStream inputStream = openStream(location)) {
			RateLimitRulesDocument document = yamlMapper.readValue(inputStream, RateLimitRulesDocument.class);
			return document.getRules().stream().map(RateLimitRuleMapper::toDomain).toList();
		}
	}

	private InputStream openStream(String location) throws IOException {
		if (location.startsWith("classpath:")) {
			Resource resource = resourceLoader.getResource(location);
			if (!resource.exists()) {
				throw new IOException("Rules resource not found: " + location);
			}
			return resource.getInputStream();
		}
		return Files.newInputStream(Path.of(location));
	}
}
