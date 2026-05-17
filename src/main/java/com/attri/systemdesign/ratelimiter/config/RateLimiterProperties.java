package com.attri.systemdesign.ratelimiter.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

	private boolean enabled = true;
	private StoreType store = StoreType.memory;
	private FailurePolicy onStoreFailure = FailurePolicy.fail_open;
	private Rules rules = new Rules();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public StoreType getStore() {
		return store;
	}

	public void setStore(StoreType store) {
		this.store = store;
	}

	public FailurePolicy getOnStoreFailure() {
		return onStoreFailure;
	}

	public void setOnStoreFailure(FailurePolicy onStoreFailure) {
		this.onStoreFailure = onStoreFailure;
	}

	public Rules getRules() {
		return rules;
	}

	public void setRules(Rules rules) {
		this.rules = rules;
	}

	public enum StoreType {
		memory,
		redis
	}

	public enum FailurePolicy {
		fail_open,
		fail_closed
	}

	public static class Rules {
		private String path = "classpath:rate-limiter/rules.yml";
		private Duration reloadInterval = Duration.ofSeconds(30);

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public Duration getReloadInterval() {
			return reloadInterval;
		}

		public void setReloadInterval(Duration reloadInterval) {
			this.reloadInterval = reloadInterval;
		}
	}
}
