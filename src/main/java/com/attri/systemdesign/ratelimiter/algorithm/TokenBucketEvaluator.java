package com.attri.systemdesign.ratelimiter.algorithm;

import org.springframework.stereotype.Component;

import com.attri.systemdesign.ratelimiter.domain.RateLimitAlgorithm;
import com.attri.systemdesign.ratelimiter.domain.RateLimitRequestContext;
import com.attri.systemdesign.ratelimiter.domain.RateLimitRule;
import com.attri.systemdesign.ratelimiter.domain.RateLimitVerdict;
import com.attri.systemdesign.ratelimiter.store.RateLimitCounterStore;
import com.attri.systemdesign.ratelimiter.store.TokenBucketState;

@Component
public class TokenBucketEvaluator implements RateLimitAlgorithmEvaluator {

	@Override
	public boolean supports(RateLimitRule rule) {
		return rule.algorithm() == RateLimitAlgorithm.TOKEN_BUCKET;
	}

	@Override
	public RateLimitVerdict evaluate(RateLimitRule rule, RateLimitRequestContext context, RateLimitCounterStore store) {
		String counterKey = buildCounterKey(rule, context);
		long capacity = rule.effectiveBucketSize();
		double refillRate = rule.effectiveRefillRate();
		long nowMillis = System.currentTimeMillis();

		TokenBucketState state = store.consumeTokenBucket(counterKey, capacity, refillRate, nowMillis);
		long remaining = Math.max(0, (long) Math.floor(state.tokens()));

		if (state.allowed()) {
			return RateLimitVerdict.allow(rule.name(), capacity, remaining);
		}

		long retryAfterSeconds = estimateRetryAfter(state.tokens(), refillRate);
		return RateLimitVerdict.deny(rule.name(), capacity, retryAfterSeconds);
	}

	private String buildCounterKey(RateLimitRule rule, RateLimitRequestContext context) {
		return "tb:" + rule.name() + ":" + context.dimensionValue(rule.key());
	}

	private long estimateRetryAfter(double tokens, double refillRate) {
		if (refillRate <= 0) {
			return 1;
		}
		double deficit = 1.0 - tokens;
		return Math.max(1, (long) Math.ceil(deficit / refillRate));
	}
}
