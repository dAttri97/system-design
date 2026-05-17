package com.attri.systemdesign.ratelimiter.domain;

public record RateLimitVerdict(
		boolean allowed,
		String ruleName,
		long limit,
		long remaining,
		long retryAfterSeconds) {

	public static RateLimitVerdict allow(String ruleName, long limit, long remaining) {
		return new RateLimitVerdict(true, ruleName, limit, remaining, 0);
	}

	public static RateLimitVerdict deny(String ruleName, long limit, long retryAfterSeconds) {
		return new RateLimitVerdict(false, ruleName, limit, 0, retryAfterSeconds);
	}
}
