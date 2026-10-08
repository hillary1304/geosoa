package com.geosoa.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("geosoa.cache")
public record CacheProperties(Duration igpTtl, Duration senamhiTtl, Duration ineiTtl, long tamanoMaximo) {
}
