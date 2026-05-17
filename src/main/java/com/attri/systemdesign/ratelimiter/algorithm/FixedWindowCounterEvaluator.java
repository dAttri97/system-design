package com.attri.systemdesign.ratelimiter.algorithm;

import org.springframework.stereotype.Component;

import com.attri.systemdesign.ratelimiter.domain.RateLimitAlgorithm;
import com.attri.systemdesign.ratelimiter.domain.RateLimitRequestContext;
import com.attri.systemdesign.ratelimiter.domain.RateLimitRule;
import com.attri.systemdesign.ratelimiter.domain.RateLimitVerdict;
import com.attri.systemdesign.ratelimiter.store.FixedWindowState;
import com.attri.systemdesign.ratelimiter.store.RateLimitCounterStore;

@Component
public class FixedWindowCounterEvaluator implements RateLimitAlgorithmEvaluator {

	@Override
	public boolean supports(RateLimitRule rule) {
		return rule.algorithm() == RateLimitAlgorithm.FIXED_WINDOW;
	}

	@Override
	public RateLimitVerdict evaluate(RateLimitRule rule, RateLimitRequestContext context, RateLimitCounterStore store) {
		String counterKey = buildCounterKey(rule, context);
		long windowMillis = rule.window().toMillis();
		long nowMillis = System.currentTimeMillis();
		long windowStart = (nowMillis / windowMillis) * windowMillis;

		FixedWindowState state = store.incrementFixedWindow(counterKey, windowStart, windowMillis, rule.limit(), nowMillis);
		long remaining = Math.max(0, rule.limit() - state.count());

		if (state.allowed()) {
			return RateLimitVerdict.allow(rule.name(), rule.limit(), remaining);
		}

		long retryAfterSeconds = Math.max(1, (windowStart + windowMillis - nowMillis + 999) / 1000);
		return RateLimitVerdict.deny(rule.name(), rule.limit(), retryAfterSeconds);
	}

	private String buildCounterKey(RateLimitRule rule, RateLimitRequestContext context) {
		return "fw:" + rule.name() + ":" + context.dimensionValue(rule.key());
	}
}
