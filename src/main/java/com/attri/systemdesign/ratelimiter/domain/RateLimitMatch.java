package com.attri.systemdesign.ratelimiter.domain;

public record RateLimitMatch(String method, String path) {
}
