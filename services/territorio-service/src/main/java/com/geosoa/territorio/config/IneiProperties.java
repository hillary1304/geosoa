package com.geosoa.territorio.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("geosoa.inei")
public record IneiProperties(URI url, Duration connectTimeout, Duration readTimeout) {
}
