package com.attri.systemdesign.ratelimiter.algorithm;

import com.attri.systemdesign.ratelimiter.domain.RateLimitRequestContext;
import com.attri.systemdesign.ratelimiter.domain.RateLimitRule;
import com.attri.systemdesign.ratelimiter.domain.RateLimitVerdict;
import com.attri.systemdesign.ratelimiter.store.RateLimitCounterStore;

public interface RateLimitAlgorithmEvaluator {
	boolean supports(RateLimitRule rule);

	RateLimitVerdict evaluate(RateLimitRule rule, RateLimitRequestContext context, RateLimitCounterStore store);
}
