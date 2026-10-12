package com.geosoa.backend.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("geosoa.igp")
public record IgpProperties(URI url, Duration connectTimeout, Duration readTimeout) {
}
