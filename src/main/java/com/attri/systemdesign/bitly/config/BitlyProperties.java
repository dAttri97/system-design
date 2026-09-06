package com.attri.systemdesign.bitly.config;

import java.time.Duration;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bitly")
public class BitlyProperties {

	private boolean enabled = true;
	private String baseUrl = "http://localhost:8080";
	private Store store = new Store();
	private Cache cache = new Cache();
	private Cleanup cleanup = new Cleanup();
	private Set<String> reservedPaths = Set.of(
			"api", "actuator", "urls", "error", "favicon.ico", "index.html", "static");

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public Store getStore() {
		return store;
	}

	public void setStore(Store store) {
		this.store = store;
	}

	public Cache getCache() {
		return cache;
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}

	public Cleanup getCleanup() {
		return cleanup;
	}

	public void setCleanup(Cleanup cleanup) {
		this.cleanup = cleanup;
	}

	public Set<String> getReservedPaths() {
		return reservedPaths;
	}

	public void setReservedPaths(Set<String> reservedPaths) {
		this.reservedPaths = reservedPaths;
	}

	public static class Store {
		private StoreType type = StoreType.memory;

		public StoreType getType() {
			return type;
		}

		public void setType(StoreType type) {
			this.type = type;
		}
	}

	public static class Cache {
		private CacheType type = CacheType.memory;
		private int maxSize = 10_000;

		public CacheType getType() {
			return type;
		}

		public void setType(CacheType type) {
			this.type = type;
		}

		public int getMaxSize() {
			return maxSize;
		}

		public void setMaxSize(int maxSize) {
			this.maxSize = maxSize;
		}
	}

	public static class Cleanup {
		private boolean enabled = true;
		private Duration interval = Duration.ofHours(1);

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Duration getInterval() {
			return interval;
		}

		public void setInterval(Duration interval) {
			this.interval = interval;
		}
	}

	public enum StoreType {
		memory,
		redis
	}

	public enum CacheType {
		memory,
		redis
	}
}
