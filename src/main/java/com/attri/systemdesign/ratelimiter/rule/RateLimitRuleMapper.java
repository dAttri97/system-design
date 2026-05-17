package com.attri.systemdesign.ratelimiter.rule;

import com.attri.systemdesign.ratelimiter.domain.RateLimitAlgorithm;
import com.attri.systemdesign.ratelimiter.domain.RateLimitDimension;
import com.attri.systemdesign.ratelimiter.domain.RateLimitMatch;
import com.attri.systemdesign.ratelimiter.domain.RateLimitRule;

public final class RateLimitRuleMapper {

	private RateLimitRuleMapper() {
	}

	public static RateLimitRule toDomain(RateLimitRulesDocument.RateLimitRuleDefinition definition) {
		RateLimitRulesDocument.MatchDefinition matchDef = definition.getMatch();
		RateLimitMatch match = new RateLimitMatch(
				matchDef != null ? matchDef.getMethod() : null,
				matchDef != null ? matchDef.getPath() : null);

		return new RateLimitRule(
				definition.getName(),
				parseDimension(definition.getKey()),
				match,
				definition.getLimit(),
				DurationParser.parse(definition.getWindow()),
				parseAlgorithm(definition.getAlgorithm()),
				definition.getBucketSize(),
				definition.getRefillRate());
	}

	private static RateLimitDimension parseDimension(String key) {
		return switch (key.toLowerCase()) {
			case "user_id" -> RateLimitDimension.USER_ID;
			case "ip" -> RateLimitDimension.IP;
			case "api_key" -> RateLimitDimension.API_KEY;
			case "endpoint" -> RateLimitDimension.ENDPOINT;
			case "global" -> RateLimitDimension.GLOBAL;
			default -> throw new IllegalArgumentException("Unknown rate limit dimension: " + key);
		};
	}

	private static RateLimitAlgorithm parseAlgorithm(String algorithm) {
		return switch (algorithm.toLowerCase()) {
			case "token_bucket" -> RateLimitAlgorithm.TOKEN_BUCKET;
			case "fixed_window" -> RateLimitAlgorithm.FIXED_WINDOW;
			default -> throw new IllegalArgumentException("Unknown rate limit algorithm: " + algorithm);
		};
	}
}
