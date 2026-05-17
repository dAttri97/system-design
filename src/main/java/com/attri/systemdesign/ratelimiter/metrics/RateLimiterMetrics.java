package com.attri.systemdesign.ratelimiter.metrics;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.attri.systemdesign.ratelimiter.domain.RateLimitVerdict;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class RateLimiterMetrics {

	private final MeterRegistry meterRegistry;
	private final Timer evaluationTimer;

	public RateLimiterMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		this.evaluationTimer = Timer.builder("rate_limiter.evaluation")
				.description("Time spent evaluating rate limits")
				.register(meterRegistry);
	}

	public RateLimitVerdict recordEvaluation(Supplier<RateLimitVerdict> supplier) {
		return evaluationTimer.record(supplier);
	}

	public void recordAllowed(String ruleName) {
		meterRegistry.counter("rate_limiter.requests.allowed", "rule", ruleName).increment();
	}

	public void recordDenied(String ruleName) {
		meterRegistry.counter("rate_limiter.requests.denied", "rule", ruleName).increment();
	}

	public void recordStoreError(String ruleName) {
		meterRegistry.counter("rate_limiter.store.errors", "rule", ruleName).increment();
	}
}
