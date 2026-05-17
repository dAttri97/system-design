package com.attri.systemdesign.ratelimiter.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimiterConfiguration {
}
