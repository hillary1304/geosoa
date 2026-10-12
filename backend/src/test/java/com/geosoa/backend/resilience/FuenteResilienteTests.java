package com.geosoa.backend.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import com.geosoa.backend.domain.Dato;
import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Origen;
import com.geosoa.backend.domain.Procedencia;
import com.geosoa.backend.shared.FuenteNoDisponibleException;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

class FuenteResilienteTests {

	private static final Instant AYER = Instant.parse("2026-10-06T12:00:00Z");

	private CircuitBreaker circuitBreaker;

	private FuenteResiliente fuente;

	private final AtomicInteger llamadas = new AtomicInteger();

	@BeforeEach
	void configurar() {
		circuitBreaker = CircuitBreaker.of("igp", CircuitBreakerConfig.custom()
			.slidingWindowSize(6)
			.minimumNumberOfCalls(6)
			.failureRateThreshold(50)
			.recordExceptions(FuenteNoDisponibleException.class)
			.build());
		Retry retry = Retry.of("igp", RetryConfig.custom()
			.maxAttempts(3)
			.waitDuration(Duration.ofMillis(1))
			.retryExceptions(FuenteNoDisponibleException.class)
			.build());
		fuente = new FuenteResiliente(Fuente.IGP, Caffeine.newBuilder().build(), circuitBreaker, retry);
	}

	@Test
	void consultaEnVivoYLuegoRespondeDesdeCache() {
		Dato<String> primero = fuente.obtener("clave", () -> contar("sismo"));
		Dato<String> segundo = fuente.obtener("clave", () -> contar("otro"));

		assertThat(primero.valor()).isEqualTo("sismo");
		assertThat(primero.procedencia().origen()).isEqualTo(Origen.LIVE);
		assertThat(segundo.valor()).isEqualTo("sismo");
		assertThat(segundo.procedencia()).isEqualTo(new Procedencia(Origen.CACHE, primero.procedencia().actualizadoEn()));
		assertThat(llamadas).hasValue(1);
	}

	@Test
	void reintentaAnteFallosTemporales() {
		Dato<String> dato = fuente.obtener("clave", () -> {
			if (llamadas.incrementAndGet() < 3) {
				throw new FuenteNoDisponibleException(Fuente.IGP, "timeout", null);
			}
			return "sismo";
		});

		assertThat(dato.valor()).isEqualTo("sismo");
		assertThat(llamadas).hasValue(3);
	}

	@Test
	void respondeConElRespaldoSiLaFuenteFalla() {
		Dato<String> respaldo = new Dato<>("almacenado", new Procedencia(Origen.DB, AYER));

		Dato<String> dato = fuente.obtener("clave", this::fallar, () -> Optional.of(respaldo));

		assertThat(dato).isEqualTo(respaldo);
		assertThat(llamadas).hasValue(3);
	}

	@Test
	void informaFuenteNoDisponibleSinRespaldo() {
		assertThatThrownBy(() -> fuente.obtener("clave", this::fallar))
			.isInstanceOfSatisfying(FuenteNoDisponibleException.class,
					ex -> assertThat(ex.getFuente()).isEqualTo(Fuente.IGP));
	}

	@Test
	void informaFuenteNoDisponibleSiFallaElRespaldo() {
		Supplier<Optional<Dato<String>>> respaldo = () -> {
			throw new DataAccessResourceFailureException("MySQL caído");
		};

		assertThatThrownBy(() -> fuente.obtener("clave", this::fallar, respaldo))
			.isInstanceOf(FuenteNoDisponibleException.class);
	}

	@Test
	void conElCircuitoAbiertoNoLlamaALaFuente() {
		circuitBreaker.transitionToOpenState();
		Dato<String> respaldo = new Dato<>("almacenado", new Procedencia(Origen.DB, AYER));

		assertThat(fuente.obtener("clave", () -> contar("sismo"), () -> Optional.of(respaldo))).isEqualTo(respaldo);
		assertThatThrownBy(() -> fuente.obtener("otra", () -> contar("sismo")))
			.isInstanceOf(FuenteNoDisponibleException.class)
			.hasMessageContaining("circuito abierto");
		assertThat(llamadas).hasValue(0);
	}

	// Cada reintento cuenta para el circuito: dos consultas fallidas suman seis llamadas.
	@Test
	void abreElCircuitoTrasVariosFallos() {
		for (int i = 0; i < 2; i++) {
			String clave = "clave" + i;
			assertThatThrownBy(() -> fuente.obtener(clave, this::fallar)).isInstanceOf(FuenteNoDisponibleException.class);
		}

		assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
	}

	@Test
	void noReintentaNiRespaldaErroresDeValidacion() {
		assertThatThrownBy(() -> fuente.obtener("clave", () -> {
			llamadas.incrementAndGet();
			throw new IllegalArgumentException("Coordenadas fuera de rango");
		}, () -> Optional.of(new Dato<>("almacenado", new Procedencia(Origen.DB, AYER)))))
			.isInstanceOf(IllegalArgumentException.class);
		assertThat(llamadas).hasValue(1);
	}

	@Test
	void combinaProcedenciasConLaMenosFresca() {
		Procedencia enVivo = new Procedencia(Origen.LIVE, Instant.parse("2026-10-07T12:00:00Z"));
		Procedencia almacenada = new Procedencia(Origen.DB, AYER);

		assertThat(enVivo.combinar(almacenada)).isEqualTo(almacenada);
		assertThat(almacenada.combinar(enVivo)).isEqualTo(almacenada);
		assertThat(enVivo.combinar(null)).isEqualTo(enVivo);
	}

	private String contar(String valor) {
		llamadas.incrementAndGet();
		return valor;
	}

	private String fallar() {
		llamadas.incrementAndGet();
		throw new FuenteNoDisponibleException(Fuente.IGP, "Error al consultar el IGP", null);
	}

}
