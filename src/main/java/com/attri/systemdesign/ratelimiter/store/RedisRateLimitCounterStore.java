package com.attri.systemdesign.ratelimiter.store;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "rate-limiter.store.type", havingValue = "redis")
public class RedisRateLimitCounterStore implements RateLimitCounterStore {

	private static final String TOKEN_BUCKET_SCRIPT = """
			local key = KEYS[1]
			local capacity = tonumber(ARGV[1])
			local refill_rate = tonumber(ARGV[2])
			local now = tonumber(ARGV[3])

			local tokens = tonumber(redis.call('HGET', key, 'tokens'))
			local last_refill = tonumber(redis.call('HGET', key, 'last_refill'))

			if tokens == nil then
			  tokens = capacity
			  last_refill = now
			end

			local elapsed = math.max(0, now - last_refill) / 1000.0
			tokens = math.min(capacity, tokens + elapsed * refill_rate)
			last_refill = now

			local allowed = 0
			if tokens >= 1 then
			  tokens = tokens - 1
			  allowed = 1
			end

			redis.call('HSET', key, 'tokens', tokens, 'last_refill', last_refill)
			local ttl = math.ceil((capacity / refill_rate) + 60)
			redis.call('EXPIRE', key, ttl)

			return {allowed, tokens}
			""";

	private static final String FIXED_WINDOW_SCRIPT = """
			local key = KEYS[1]
			local limit = tonumber(ARGV[1])
			local ttl_seconds = tonumber(ARGV[2])

			local count = redis.call('INCR', key)
			if count == 1 then
			  redis.call('EXPIRE', key, ttl_seconds)
			end

			local allowed = 0
			if count <= limit then
			  allowed = 1
			end

			return {allowed, count}
			""";

	private final StringRedisTemplate redisTemplate;
	private final DefaultRedisScript<List> tokenBucketScript;
	private final DefaultRedisScript<List> fixedWindowScript;

	public RedisRateLimitCounterStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
		this.tokenBucketScript = new DefaultRedisScript<>(TOKEN_BUCKET_SCRIPT, List.class);
		this.fixedWindowScript = new DefaultRedisScript<>(FIXED_WINDOW_SCRIPT, List.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public TokenBucketState consumeTokenBucket(String key, long capacity, double refillRatePerSecond, long nowMillis) {
		List<Number> result = redisTemplate.execute(
				tokenBucketScript,
				List.of(key),
				String.valueOf(capacity),
				String.valueOf(refillRatePerSecond),
				String.valueOf(nowMillis));
		boolean allowed = result.get(0).longValue() == 1L;
		double tokens = result.get(1).doubleValue();
		return new TokenBucketState(allowed, tokens);
	}

	@Override
	@SuppressWarnings("unchecked")
	public FixedWindowState incrementFixedWindow(
			String key,
			long windowStartMillis,
			long windowMillis,
			long limit,
			long nowMillis) {
		String redisKey = key + ":" + windowStartMillis;
		long ttlSeconds = Math.max(1, (windowMillis + 999) / 1000);
		List<Number> result = redisTemplate.execute(
				fixedWindowScript,
				List.of(redisKey),
				String.valueOf(limit),
				String.valueOf(ttlSeconds));
		boolean allowed = result.get(0).longValue() == 1L;
		long count = result.get(1).longValue();
		return new FixedWindowState(allowed, count);
	}
}
