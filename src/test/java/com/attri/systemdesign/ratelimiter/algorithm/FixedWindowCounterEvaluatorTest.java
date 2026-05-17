package com.attri.systemdesign.ratelimiter.algorithm;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.attri.systemdesign.ratelimiter.domain.RateLimitAlgorithm;
import com.attri.systemdesign.ratelimiter.domain.RateLimitDimension;
import com.attri.systemdesign.ratelimiter.domain.RateLimitMatch;
import com.attri.systemdesign.ratelimiter.domain.RateLimitRequestContext;
import com.attri.systemdesign.ratelimiter.domain.RateLimitRule;
import com.attri.systemdesign.ratelimiter.store.InMemoryRateLimitCounterStore;

class FixedWindowCounterEvaluatorTest {

	private FixedWindowCounterEvaluator evaluator;
	private InMemoryRateLimitCounterStore store;

	@BeforeEach
	void setUp() {
		evaluator = new FixedWindowCounterEvaluator();
		store = new InMemoryRateLimitCounterStore();
	}

	@Test
	void blocksAfterLimitWithinWindow() {
		RateLimitRule rule = new RateLimitRule(
				"login_attempts",
				RateLimitDimension.IP,
				new RateLimitMatch("POST", "/api/auth/login"),
				2,
				Duration.ofMinutes(1),
				RateLimitAlgorithm.FIXED_WINDOW,
				null,
				null);
		RateLimitRequestContext context = new RateLimitRequestContext("POST", "/api/auth/login", null, "10.0.0.1", null);

		assertThat(evaluator.evaluate(rule, context, store).allowed()).isTrue();
		assertThat(evaluator.evaluate(rule, context, store).allowed()).isTrue();
		assertThat(evaluator.evaluate(rule, context, store).allowed()).isFalse();
	}
}
