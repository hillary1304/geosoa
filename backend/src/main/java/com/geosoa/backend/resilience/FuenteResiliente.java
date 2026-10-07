package com.geosoa.backend.resilience;

import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

import com.geosoa.backend.domain.Dato;
import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Origen;
import com.geosoa.backend.domain.Procedencia;
import com.geosoa.backend.shared.FuenteNoDisponibleException;
import com.github.benmanes.caffeine.cache.Cache;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

public class FuenteResiliente {

	private static final Logger log = LoggerFactory.getLogger(FuenteResiliente.class);

	private final Fuente fuente;

	private final Cache<Object, Dato<?>> cache;

	private final CircuitBreaker circuitBreaker;

	private final Retry retry;

	public FuenteResiliente(Fuente fuente, Cache<Object, Dato<?>> cache, CircuitBreaker circuitBreaker, Retry retry) {
		this.fuente = fuente;
		this.cache = cache;
		this.circuitBreaker = circuitBreaker;
		this.retry = retry;
	}

	public <T> Dato<T> obtener(Object clave, Supplier<T> enVivo) {
		return obtener(clave, enVivo, Optional::empty);
	}

	// Orden: caché, fuente en vivo con reintentos y circuit breaker, y por último el respaldo en MySQL.
	@SuppressWarnings("unchecked")
	public <T> Dato<T> obtener(Object clave, Supplier<T> enVivo, Supplier<Optional<Dato<T>>> respaldo) {
		Dato<T> enCache = (Dato<T>) cache.getIfPresent(clave);
		if (enCache != null) {
			return enCache.conOrigen(Origen.CACHE);
		}
		try {
			T valor = Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, enVivo)).get();
			Dato<T> dato = new Dato<>(valor, Procedencia.enVivo());
			cache.put(clave, dato);
			return dato;
		}
		catch (FuenteNoDisponibleException | CallNotPermittedException ex) {
			return respaldar(respaldo, ex);
		}
	}

	private <T> Dato<T> respaldar(Supplier<Optional<Dato<T>>> respaldo, RuntimeException causa) {
		Optional<Dato<T>> almacenado;
		try {
			almacenado = respaldo.get();
		}
		catch (DataAccessException ex) {
			log.warn("No se pudo leer el respaldo de {}", fuente, ex);
			almacenado = Optional.empty();
		}
		if (almacenado.isPresent()) {
			log.warn("{} no disponible, se responde con el respaldo en MySQL: {}", fuente, causa.getMessage());
			return almacenado.get();
		}
		if (causa instanceof FuenteNoDisponibleException noDisponible) {
			throw noDisponible;
		}
		throw new FuenteNoDisponibleException(fuente, fuente + " no disponible (circuito abierto)", causa);
	}

}
