package com.attri.systemdesign.bitly.store;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.attri.systemdesign.bitly.domain.UrlMapping;

@Repository
@ConditionalOnProperty(name = "bitly.store.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryUrlMappingRepository implements UrlMappingRepository {

	private final ConcurrentHashMap<String, UrlMapping> mappings = new ConcurrentHashMap<>();

	@Override
	public UrlMapping save(UrlMapping mapping) {
		if (mappings.putIfAbsent(mapping.shortCode(), mapping) != null) {
			throw new IllegalStateException("short code already exists: " + mapping.shortCode());
		}
		return mapping;
	}

	@Override
	public Optional<UrlMapping> findByShortCode(String shortCode) {
		return Optional.ofNullable(mappings.get(shortCode));
	}

	@Override
	public boolean existsByShortCode(String shortCode) {
		return mappings.containsKey(shortCode);
	}

	@Override
	public int deleteExpiredBefore(Instant cutoff) {
		int removed = 0;
		for (var entry : mappings.entrySet()) {
			UrlMapping mapping = entry.getValue();
			if (mapping.expirationDate() != null && mapping.expirationDate().isBefore(cutoff)) {
				if (mappings.remove(entry.getKey(), mapping)) {
					removed++;
				}
			}
		}
		return removed;
	}
}
