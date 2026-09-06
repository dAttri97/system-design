package com.attri.systemdesign.bitly.job;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.attri.systemdesign.bitly.config.BitlyProperties;
import com.attri.systemdesign.bitly.store.UrlMappingRepository;

@Component
@ConditionalOnProperty(name = "bitly.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class ExpiredUrlCleanupJob {

	private static final Logger log = LoggerFactory.getLogger(ExpiredUrlCleanupJob.class);

	private final UrlMappingRepository repository;
	private final BitlyProperties properties;

	public ExpiredUrlCleanupJob(UrlMappingRepository repository, BitlyProperties properties) {
		this.repository = repository;
		this.properties = properties;
	}

	@Scheduled(fixedDelayString = "${bitly.cleanup.interval-ms:3600000}")
	public void cleanupExpiredUrls() {
		if (!properties.isEnabled()) {
			return;
		}
		int removed = repository.deleteExpiredBefore(Instant.now());
		if (removed > 0) {
			log.info("Removed {} expired URL mappings", removed);
		}
	}
}
