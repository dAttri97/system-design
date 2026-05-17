package com.attri.systemdesign.ratelimiter.rule;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.attri.systemdesign.ratelimiter.config.RateLimiterProperties;
import com.attri.systemdesign.ratelimiter.domain.RateLimitRule;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class RateLimitRuleCache {

	private static final Logger log = LoggerFactory.getLogger(RateLimitRuleCache.class);

	private final RateLimitRuleLoader loader;
	private final AtomicReference<List<RateLimitRule>> rules = new AtomicReference<>(List.of());

	public RateLimitRuleCache(RateLimitRuleLoader loader, RateLimiterProperties properties, MeterRegistry meterRegistry) {
		this.loader = loader;
		reload();
		meterRegistry.gauge("rate_limiter.rules.count", rules, ref -> ref.get().size());
	}

	public List<RateLimitRule> getRules() {
		return rules.get();
	}

	public void reload() {
		try {
			List<RateLimitRule> loaded = loader.load();
			rules.set(Collections.unmodifiableList(loaded));
			log.info("Loaded {} rate limit rules", loaded.size());
		}
		catch (Exception exception) {
			log.error("Failed to reload rate limit rules", exception);
		}
	}
}
