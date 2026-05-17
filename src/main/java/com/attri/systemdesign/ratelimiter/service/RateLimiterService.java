package com.attri.systemdesign.ratelimiter.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.attri.systemdesign.ratelimiter.algorithm.RateLimitAlgorithmEvaluator;
import com.attri.systemdesign.ratelimiter.config.RateLimiterProperties;
import com.attri.systemdesign.ratelimiter.domain.RateLimitRequestContext;
import com.attri.systemdesign.ratelimiter.domain.RateLimitRule;
import com.attri.systemdesign.ratelimiter.domain.RateLimitVerdict;
import com.attri.systemdesign.ratelimiter.metrics.RateLimiterMetrics;
import com.attri.systemdesign.ratelimiter.rule.RateLimitRuleCache;
import com.attri.systemdesign.ratelimiter.store.RateLimitCounterStore;

@Service
public class RateLimiterService {

	private final RateLimitRuleCache ruleCache;
	private final RateLimitCounterStore counterStore;
	private final List<RateLimitAlgorithmEvaluator> evaluators;
	private final RateLimiterProperties properties;
	private final RateLimiterMetrics metrics;

	public RateLimiterService(
			RateLimitRuleCache ruleCache,
			RateLimitCounterStore counterStore,
			List<RateLimitAlgorithmEvaluator> evaluators,
			RateLimiterProperties properties,
			RateLimiterMetrics metrics) {
		this.ruleCache = ruleCache;
		this.counterStore = counterStore;
		this.evaluators = evaluators;
		this.properties = properties;
		this.metrics = metrics;
	}

	public RateLimitVerdict check(RateLimitRequestContext context) {
		return metrics.recordEvaluation(() -> evaluate(context));
	}

	private RateLimitVerdict evaluate(RateLimitRequestContext context) {
		List<RateLimitRule> rules = ruleCache.getRules();
		RateLimitVerdict tightestAllow = null;

		for (RateLimitRule rule : rules) {
			if (!rule.matches(context.method(), context.path())) {
				continue;
			}

			try {
				RateLimitVerdict verdict = evaluateRule(rule, context);
				if (!verdict.allowed()) {
					metrics.recordDenied(rule.name());
					return verdict;
				}
				metrics.recordAllowed(rule.name());
				tightestAllow = pickTighterAllow(tightestAllow, verdict);
			}
			catch (Exception exception) {
				metrics.recordStoreError(rule.name());
				if (properties.getOnStoreFailure() == RateLimiterProperties.FailurePolicy.fail_closed) {
					return RateLimitVerdict.deny(rule.name(), rule.limit(), 60);
				}
				metrics.recordAllowed(rule.name());
			}
		}

		if (tightestAllow != null) {
			return tightestAllow;
		}
		return RateLimitVerdict.allow("none", Long.MAX_VALUE, Long.MAX_VALUE);
	}

	private RateLimitVerdict evaluateRule(RateLimitRule rule, RateLimitRequestContext context) {
		for (RateLimitAlgorithmEvaluator evaluator : evaluators) {
			if (evaluator.supports(rule)) {
				return evaluator.evaluate(rule, context, counterStore);
			}
		}
		throw new IllegalStateException("No evaluator registered for rule: " + rule.name());
	}

	private RateLimitVerdict pickTighterAllow(RateLimitVerdict current, RateLimitVerdict candidate) {
		if (current == null) {
			return candidate;
		}
		if (candidate.remaining() < current.remaining()) {
			return candidate;
		}
		return current;
	}
}
