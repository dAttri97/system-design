package com.attri.systemdesign.bitly.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(BitlyProperties.class)
@ConditionalOnProperty(name = "bitly.enabled", havingValue = "true", matchIfMissing = true)
public class BitlyConfiguration {
}
