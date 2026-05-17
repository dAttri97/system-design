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
import com.attri.systemdesign.ratelimiter.domain.RateLimitVerdict;
import com.attri.systemdesign.ratelimiter.store.InMemoryRateLimitCounterStore;

class TokenBucketEvaluatorTest {

	private TokenBucketEvaluator evaluator;
	private InMemoryRateLimitCounterStore store;

	@BeforeEach
	void setUp() {
		evaluator = new TokenBucketEvaluator();
		store = new InMemoryRateLimitCounterStore();
	}

	@Test
	void allowsBurstUpToBucketSize() {
		RateLimitRule rule = new RateLimitRule(
				"write_posts",
				RateLimitDimension.USER_ID,
				new RateLimitMatch("POST", "/api/posts"),
				2,
				Duration.ofSeconds(1),
				RateLimitAlgorithm.TOKEN_BUCKET,
				2L,
				2.0);
		RateLimitRequestContext context = new RateLimitRequestContext("POST", "/api/posts", "user-1", "127.0.0.1", null);

		assertThat(evaluator.evaluate(rule, context, store).allowed()).isTrue();
		assertThat(evaluator.evaluate(rule, context, store).allowed()).isTrue();
		assertThat(evaluator.evaluate(rule, context, store).allowed()).isFalse();
	}

	@Test
	void denyIncludesRetryAfter() {
		RateLimitRule rule = new RateLimitRule(
				"write_posts",
				RateLimitDimension.USER_ID,
				new RateLimitMatch("POST", "/api/posts"),
				1,
				Duration.ofSeconds(1),
				RateLimitAlgorithm.TOKEN_BUCKET,
				1L,
				1.0);
		RateLimitRequestContext context = new RateLimitRequestContext("POST", "/api/posts", "user-2", "127.0.0.1", null);

		evaluator.evaluate(rule, context, store);
		RateLimitVerdict verdict = evaluator.evaluate(rule, context, store);

		assertThat(verdict.allowed()).isFalse();
		assertThat(verdict.retryAfterSeconds()).isGreaterThanOrEqualTo(1);
	}
}
