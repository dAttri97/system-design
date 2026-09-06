package com.attri.systemdesign.bitly.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.attri.systemdesign.bitly.config.BitlyProperties;
import com.attri.systemdesign.bitly.domain.UrlMapping;
import com.attri.systemdesign.bitly.exception.AliasConflictException;
import com.attri.systemdesign.bitly.exception.InvalidUrlException;
import com.attri.systemdesign.bitly.store.UrlCache;
import com.attri.systemdesign.bitly.store.UrlMappingRepository;
import com.attri.systemdesign.bitly.util.ShortCodeGenerator;
import com.attri.systemdesign.bitly.util.UrlValidator;

@Service
public class UrlShortenerService {

	private static final int MAX_GENERATION_RETRIES = 5;

	private final UrlMappingRepository repository;
	private final UrlCache cache;
	private final ShortCodeGenerator shortCodeGenerator;
	private final BitlyProperties properties;

	public UrlShortenerService(
			UrlMappingRepository repository,
			UrlCache cache,
			ShortCodeGenerator shortCodeGenerator,
			BitlyProperties properties) {
		this.repository = repository;
		this.cache = cache;
		this.shortCodeGenerator = shortCodeGenerator;
		this.properties = properties;
	}

	public String shorten(String longUrl, String customAlias, Instant expirationDate) {
		if (!UrlValidator.isValidLongUrl(longUrl)) {
			throw new InvalidUrlException("Invalid URL. Only http and https URLs are supported.");
		}
		if (expirationDate != null && expirationDate.isBefore(Instant.now())) {
			throw new InvalidUrlException("Expiration date must be in the future.");
		}

		String shortCode = resolveShortCode(customAlias);
		UrlMapping mapping = new UrlMapping(shortCode, longUrl.trim(), expirationDate, Instant.now());

		try {
			repository.save(mapping);
		}
		catch (IllegalStateException ex) {
			throw new AliasConflictException(shortCode);
		}

		cache.put(mapping);
		return buildShortUrl(shortCode);
	}

	private String resolveShortCode(String customAlias) {
		if (customAlias == null || customAlias.isBlank()) {
			for (int attempt = 0; attempt < MAX_GENERATION_RETRIES; attempt++) {
				String generated = shortCodeGenerator.generate();
				if (!repository.existsByShortCode(generated)) {
					return generated;
				}
			}
			throw new IllegalStateException("Unable to generate a unique short code");
		}

		String alias = customAlias.trim();
		if (!UrlValidator.isValidCustomAlias(alias)) {
			throw new InvalidUrlException(
					"Custom alias must be 3-50 characters and contain only letters, numbers, hyphens, or underscores.");
		}
		if (properties.getReservedPaths().contains(alias)) {
			throw new InvalidUrlException("Custom alias is reserved.");
		}
		if (repository.existsByShortCode(alias)) {
			throw new AliasConflictException(alias);
		}
		return alias;
	}

	private String buildShortUrl(String shortCode) {
		String baseUrl = properties.getBaseUrl().replaceAll("/$", "");
		return baseUrl + "/" + shortCode;
	}
}
