package com.geosoa.meteorologia.resilience;

import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geosoa.meteorologia.domain.Dato;
import com.geosoa.meteorologia.domain.Fuente;
import com.geosoa.meteorologia.domain.Origen;
import com.geosoa.meteorologia.domain.Procedencia;
import com.geosoa.meteorologia.shared.FuenteNoDisponibleException;
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

	@SuppressWarnings("unchecked")
	public <T> Dato<T> obtener(Object clave, Supplier<T> enVivo) {
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
			log.warn("{} no disponible y sin resultado en caché: {}", fuente, ex.getMessage());
			if (ex instanceof FuenteNoDisponibleException noDisponible) {
				throw noDisponible;
			}
			throw new FuenteNoDisponibleException(fuente, fuente + " no disponible (circuito abierto)", ex);
		}
	}

}
