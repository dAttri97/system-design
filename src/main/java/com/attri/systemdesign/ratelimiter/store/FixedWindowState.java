package com.attri.systemdesign.ratelimiter.store;

public record FixedWindowState(boolean allowed, long count) {
}
