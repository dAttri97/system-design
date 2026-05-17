package com.attri.systemdesign.ratelimiter.store;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "rate-limiter.store.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryRateLimitCounterStore implements RateLimitCounterStore {

	private final ConcurrentMap<String, TokenBucketEntry> tokenBuckets = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, FixedWindowEntry> fixedWindows = new ConcurrentHashMap<>();

	@Override
	public TokenBucketState consumeTokenBucket(String key, long capacity, double refillRatePerSecond, long nowMillis) {
		TokenBucketEntry entry = tokenBuckets.computeIfAbsent(key, ignored -> new TokenBucketEntry(capacity, nowMillis));
		synchronized (entry) {
			refill(entry, capacity, refillRatePerSecond, nowMillis);
			if (entry.tokens >= 1.0d) {
				entry.tokens -= 1.0d;
				return new TokenBucketState(true, entry.tokens);
			}
			return new TokenBucketState(false, entry.tokens);
		}
	}

	@Override
	public FixedWindowState incrementFixedWindow(
			String key,
			long windowStartMillis,
			long windowMillis,
			long limit,
			long nowMillis) {
		String windowKey = key + ":" + windowStartMillis;
		FixedWindowEntry entry = fixedWindows.computeIfAbsent(windowKey, ignored -> new FixedWindowEntry(windowStartMillis, windowMillis));
		synchronized (entry) {
			if (entry.expiresAtMillis <= nowMillis) {
				fixedWindows.remove(windowKey);
				entry = fixedWindows.computeIfAbsent(windowKey, ignored -> new FixedWindowEntry(windowStartMillis, windowMillis));
			}
			entry.count++;
			boolean allowed = entry.count <= limit;
			return new FixedWindowState(allowed, entry.count);
		}
	}

	private void refill(TokenBucketEntry entry, long capacity, double refillRatePerSecond, long nowMillis) {
		double elapsedSeconds = Math.max(0, nowMillis - entry.lastRefillMillis) / 1000.0d;
		entry.tokens = Math.min(capacity, entry.tokens + elapsedSeconds * refillRatePerSecond);
		entry.lastRefillMillis = nowMillis;
	}

	private static final class TokenBucketEntry {
		private double tokens;
		private long lastRefillMillis;

		private TokenBucketEntry(long capacity, long nowMillis) {
			this.tokens = capacity;
			this.lastRefillMillis = nowMillis;
		}
	}

	private static final class FixedWindowEntry {
		private long count;
		private final long expiresAtMillis;

		private FixedWindowEntry(long windowStartMillis, long windowMillis) {
			this.count = 0;
			this.expiresAtMillis = windowStartMillis + windowMillis;
		}
	}
}
