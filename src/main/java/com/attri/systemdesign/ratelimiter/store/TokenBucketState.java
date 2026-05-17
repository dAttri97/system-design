package com.attri.systemdesign.ratelimiter.store;

public record TokenBucketState(boolean allowed, double tokens) {
}
