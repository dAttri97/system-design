package com.attri.systemdesign.bitly.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.attri.systemdesign.bitly.domain.UrlMapping;
import com.attri.systemdesign.bitly.exception.UrlExpiredException;
import com.attri.systemdesign.bitly.exception.UrlNotFoundException;
import com.attri.systemdesign.bitly.store.UrlCache;
import com.attri.systemdesign.bitly.store.UrlMappingRepository;

@Service
public class RedirectService {

	private final UrlMappingRepository repository;
	private final UrlCache cache;

	public RedirectService(UrlMappingRepository repository, UrlCache cache) {
		this.repository = repository;
		this.cache = cache;
	}

	public String resolveLongUrl(String shortCode) {
		Instant now = Instant.now();

		return cache.get(shortCode)
				.or(() -> repository.findByShortCode(shortCode))
				.map(mapping -> validateAndCache(mapping, now))
				.orElseThrow(() -> new UrlNotFoundException(shortCode));
	}

	private String validateAndCache(UrlMapping mapping, Instant now) {
		if (mapping.isExpired(now)) {
			cache.invalidate(mapping.shortCode());
			throw new UrlExpiredException(mapping.shortCode());
		}
		cache.put(mapping);
		return mapping.longUrl();
	}
}
