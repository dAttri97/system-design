package com.attri.systemdesign.ratelimiter.domain;

import java.time.Duration;

public record RateLimitRule(
		String name,
		RateLimitDimension key,
		RateLimitMatch match,
		long limit,
		Duration window,
		RateLimitAlgorithm algorithm,
		Long bucketSize,
		Double refillRate) {

	public boolean matches(String method, String path) {
		if (match.method() != null && !match.method().equalsIgnoreCase(method)) {
			return false;
		}
		if (match.path() == null) {
			return true;
		}
		String pattern = match.path();
		if (pattern.endsWith("/**")) {
			String prefix = pattern.substring(0, pattern.length() - 3);
			return path.startsWith(prefix);
		}
		if (pattern.endsWith("/*")) {
			String prefix = pattern.substring(0, pattern.length() - 2);
			int nextSlash = path.indexOf('/', prefix.length());
			return path.startsWith(prefix) && (nextSlash < 0 || path.length() == prefix.length());
		}
		return path.equals(pattern);
	}

	public long effectiveBucketSize() {
		return bucketSize != null ? bucketSize : limit;
	}

	public double effectiveRefillRate() {
		if (refillRate != null) {
			return refillRate;
		}
		double windowSeconds = Math.max(1, window.toMillis()) / 1000.0;
		return limit / windowSeconds;
	}
}
