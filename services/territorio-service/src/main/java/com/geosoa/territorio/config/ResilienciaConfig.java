package com.geosoa.territorio.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.geosoa.territorio.domain.Dato;
import com.geosoa.territorio.domain.Fuente;
import com.geosoa.territorio.resilience.FuenteResiliente;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;

@Configuration
public class ResilienciaConfig {

	@Bean
	FuenteResiliente fuenteInei(CircuitBreakerRegistry circuitBreakers, RetryRegistry retries) {
		Cache<Object, Dato<?>> cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(24)).maximumSize(1000)
			.build();
		return new FuenteResiliente(Fuente.INEI, cache, circuitBreakers.circuitBreaker("inei"), retries.retry("inei"));
	}

}
