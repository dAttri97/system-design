package com.attri.systemdesign.bitly.domain;

import java.time.Instant;

public record UrlMapping(
		String shortCode,
		String longUrl,
		Instant expirationDate,
		Instant createdAt) {

	public boolean isExpired(Instant now) {
		return expirationDate != null && now.isAfter(expirationDate);
	}
}
