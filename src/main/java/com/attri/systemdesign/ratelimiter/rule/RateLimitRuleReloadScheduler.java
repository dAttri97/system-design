package com.attri.systemdesign.ratelimiter.rule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RateLimitRuleReloadScheduler {

	private final RateLimitRuleCache ruleCache;

	public RateLimitRuleReloadScheduler(RateLimitRuleCache ruleCache) {
		this.ruleCache = ruleCache;
	}

	@Scheduled(fixedDelayString = "${rate-limiter.rules.reload-interval}")
	public void reloadRules() {
		ruleCache.reload();
	}
}
