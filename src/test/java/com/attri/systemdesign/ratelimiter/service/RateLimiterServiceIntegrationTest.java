package com.attri.systemdesign.ratelimiter.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.attri.systemdesign.ratelimiter.domain.RateLimitRequestContext;

@SpringBootTest
class RateLimiterServiceIntegrationTest {

	@Autowired
	private RateLimiterService rateLimiterService;

	@Test
	void deniesThirdWritePostForSameUser() {
		RateLimitRequestContext context = new RateLimitRequestContext(
				"POST", "/api/posts", "service-test-user", "127.0.0.1", null);

		assertThat(rateLimiterService.check(context).allowed()).isTrue();
		assertThat(rateLimiterService.check(context).allowed()).isTrue();
		assertThat(rateLimiterService.check(context).allowed()).isFalse();
	}
}
