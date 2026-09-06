package com.attri.systemdesign.bitly.store;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.attri.systemdesign.bitly.config.BitlyProperties;
import com.attri.systemdesign.bitly.domain.UrlMapping;

@Component
@ConditionalOnProperty(name = "bitly.cache.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryUrlCache implements UrlCache {

	private final int maxSize;
	private final Map<String, CacheEntry> cache;

	public InMemoryUrlCache(BitlyProperties properties) {
		this.maxSize = properties.getCache().getMaxSize();
		this.cache = new LinkedHashMap<>(16, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
				return size() > maxSize;
			}
		};
	}

	@Override
	public synchronized void put(UrlMapping mapping) {
		cache.put(mapping.shortCode(), new CacheEntry(mapping, expiresAt(mapping)));
	}

	@Override
	public synchronized Optional<UrlMapping> get(String shortCode) {
		CacheEntry entry = cache.get(shortCode);
		if (entry == null) {
			return Optional.empty();
		}
		if (entry.expiresAt() != null && Instant.now().isAfter(entry.expiresAt())) {
			cache.remove(shortCode);
			return Optional.empty();
		}
		return Optional.of(entry.mapping());
	}

	@Override
	public synchronized void invalidate(String shortCode) {
		cache.remove(shortCode);
	}

	private Instant expiresAt(UrlMapping mapping) {
		if (mapping.expirationDate() != null) {
			return mapping.expirationDate();
		}
		return null;
	}

	private record CacheEntry(UrlMapping mapping, Instant expiresAt) {
	}
}
