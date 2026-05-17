package com.attri.systemdesign.ratelimiter.store;

public interface RateLimitCounterStore {

	TokenBucketState consumeTokenBucket(String key, long capacity, double refillRatePerSecond, long nowMillis);

	FixedWindowState incrementFixedWindow(
			String key,
			long windowStartMillis,
			long windowMillis,
			long limit,
			long nowMillis);
}
