package com.geosoa.backend.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.geosoa.backend.domain.Dato;
import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.resilience.FuenteResiliente;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;

@Configuration
public class ResilienciaConfig {

	@Bean
	FuenteResiliente fuenteIgp(CacheProperties cache, CircuitBreakerRegistry circuitBreakers, RetryRegistry retries) {
		return fuente(Fuente.IGP, cache.igpTtl(), cache.tamanoMaximo(), circuitBreakers, retries);
	}

	@Bean
	FuenteResiliente fuenteSenamhi(CacheProperties cache, CircuitBreakerRegistry circuitBreakers,
			RetryRegistry retries) {
		return fuente(Fuente.SENAMHI, cache.senamhiTtl(), cache.tamanoMaximo(), circuitBreakers, retries);
	}

	@Bean
	FuenteResiliente fuenteInei(CacheProperties cache, CircuitBreakerRegistry circuitBreakers, RetryRegistry retries) {
		return fuente(Fuente.INEI, cache.ineiTtl(), cache.tamanoMaximo(), circuitBreakers, retries);
	}

	// Las instancias de Resilience4j se llaman como la fuente en minúsculas (igp, senamhi, inei).
	private static FuenteResiliente fuente(Fuente fuente, Duration ttl, long tamanoMaximo,
			CircuitBreakerRegistry circuitBreakers, RetryRegistry retries) {
		Cache<Object, Dato<?>> cache = Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(tamanoMaximo).build();
		String nombre = fuente.name().toLowerCase();
		return new FuenteResiliente(fuente, cache, circuitBreakers.circuitBreaker(nombre), retries.retry(nombre));
	}

}
