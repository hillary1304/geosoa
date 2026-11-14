package com.geosoa.meteorologia.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.geosoa.meteorologia.domain.Dato;
import com.geosoa.meteorologia.domain.Fuente;
import com.geosoa.meteorologia.resilience.FuenteResiliente;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;

@Configuration
public class ResilienciaConfig {

	@Bean
	FuenteResiliente fuenteSenamhi(CircuitBreakerRegistry circuitBreakers, RetryRegistry retries) {
		Cache<Object, Dato<?>> cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(12)).maximumSize(1000)
			.build();
		return new FuenteResiliente(Fuente.SENAMHI, cache, circuitBreakers.circuitBreaker("senamhi"),
				retries.retry("senamhi"));
	}

}
