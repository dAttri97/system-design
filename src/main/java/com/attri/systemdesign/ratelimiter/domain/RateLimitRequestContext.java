package com.attri.systemdesign.ratelimiter.domain;

public record RateLimitRequestContext(
		String method,
		String path,
		String userId,
		String ip,
		String apiKey) {

	public String dimensionValue(RateLimitDimension dimension) {
		return switch (dimension) {
			case USER_ID -> userId != null ? userId : "anonymous";
			case IP -> ip;
			case API_KEY -> apiKey != null ? apiKey : "anonymous";
			case ENDPOINT -> method + ":" + path;
			case GLOBAL -> "global";
		};
	}
}
