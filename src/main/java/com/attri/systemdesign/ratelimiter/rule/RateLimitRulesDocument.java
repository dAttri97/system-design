package com.attri.systemdesign.ratelimiter.rule;

import java.util.ArrayList;
import java.util.List;

public class RateLimitRulesDocument {

	private List<RateLimitRuleDefinition> rules = new ArrayList<>();

	public List<RateLimitRuleDefinition> getRules() {
		return rules;
	}

	public void setRules(List<RateLimitRuleDefinition> rules) {
		this.rules = rules;
	}

	public static class RateLimitRuleDefinition {
		private String name;
		private String key;
		private MatchDefinition match;
		private long limit;
		private String window;
		private String algorithm;
		private Long bucketSize;
		private Double refillRate;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public MatchDefinition getMatch() {
			return match;
		}

		public void setMatch(MatchDefinition match) {
			this.match = match;
		}

		public long getLimit() {
			return limit;
		}

		public void setLimit(long limit) {
			this.limit = limit;
		}

		public String getWindow() {
			return window;
		}

		public void setWindow(String window) {
			this.window = window;
		}

		public String getAlgorithm() {
			return algorithm;
		}

		public void setAlgorithm(String algorithm) {
			this.algorithm = algorithm;
		}

		public Long getBucketSize() {
			return bucketSize;
		}

		public void setBucketSize(Long bucketSize) {
			this.bucketSize = bucketSize;
		}

		public Double getRefillRate() {
			return refillRate;
		}

		public void setRefillRate(Double refillRate) {
			this.refillRate = refillRate;
		}
	}

	public static class MatchDefinition {
		private String method;
		private String path;

		public String getMethod() {
			return method;
		}

		public void setMethod(String method) {
			this.method = method;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}
	}
}
