package com.geosoa.consulta.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("geosoa.servicios")
public record ServiciosProperties(URI sismosUrl, URI meteorologiaUrl, URI territorioUrl, Duration connectTimeout,
		Duration readTimeout, Duration plazoEvento) {
}
