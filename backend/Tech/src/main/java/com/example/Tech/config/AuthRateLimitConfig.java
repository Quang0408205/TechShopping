package com.example.Tech.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AuthRateLimitProperties.class, GoogleProperties.class})
public class AuthRateLimitConfig {
}
