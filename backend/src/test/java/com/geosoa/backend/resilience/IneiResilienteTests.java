package com.geosoa.backend.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.geosoa.backend.adapter.inei.DistritoPoblado;
import com.geosoa.backend.adapter.inei.IneiAdapter;
import com.geosoa.backend.adapter.inei.IneiRespaldo;
import com.geosoa.backend.domain.Dato;
import com.geosoa.backend.domain.Distrito;
import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Origen;
import com.geosoa.backend.domain.Poblacion;
import com.geosoa.backend.domain.Procedencia;
import com.geosoa.backend.shared.FuenteNoDisponibleException;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

class IneiResilienteTests {

	private static final Distrito LIMA = new Distrito("150101", "Lima", "Lima", "Lima");

	private static final Poblacion POBLACION_LIMA = new Poblacion("150101", 273142, 12647.6);

	private static final Procedencia ALMACENADA = new Procedencia(Origen.DB, Instant.parse("2026-10-06T12:00:00Z"));

	private IneiAdapter adapter;

	private IneiRespaldo respaldo;

	private IneiResiliente inei;

	@BeforeEach
	void configurar() {
		adapter = mock(IneiAdapter.class);
		respaldo = mock(IneiRespaldo.class);
		FuenteResiliente fuente = new FuenteResiliente(Fuente.INEI, Caffeine.newBuilder().build(),
				CircuitBreaker.ofDefaults("inei"), Retry.of("inei", RetryConfig.custom().maxAttempts(1).build()));
		inei = new IneiResiliente(adapter, fuente, respaldo);
	}

	@Test
	void consultaEnVivoSinTocarElRespaldo() {
		when(adapter.distrito(-12.05, -77.03)).thenReturn(Optional.of(LIMA));

		Dato<Optional<Distrito>> dato = inei.distrito(-12.05, -77.03);

		assertThat(dato.valor()).contains(LIMA);
		assertThat(dato.procedencia().origen()).isEqualTo(Origen.LIVE);
		verify(respaldo, never()).distrito(-12.05, -77.03);
	}

	@Test
	void conElIneiCaidoRespondeDesdeMySql() {
		when(adapter.distrito(-12.05, -77.03)).thenThrow(caido());
		when(adapter.poblacion("150101")).thenThrow(caido());
		when(adapter.distritosEnRadio(-12.05, -77.03, 20)).thenThrow(caido());
		when(respaldo.distrito(-12.05, -77.03)).thenReturn(Optional.of(new Dato<>(Optional.of(LIMA), ALMACENADA)));
		when(respaldo.poblacion("150101"))
			.thenReturn(Optional.of(new Dato<>(Optional.of(POBLACION_LIMA), ALMACENADA)));
		List<DistritoPoblado> enRadio = List.of(new DistritoPoblado(LIMA, POBLACION_LIMA));
		when(respaldo.distritosEnRadio(-12.05, -77.03, 20)).thenReturn(Optional.of(new Dato<>(enRadio, ALMACENADA)));

		assertThat(inei.distrito(-12.05, -77.03)).isEqualTo(new Dato<>(Optional.of(LIMA), ALMACENADA));
		assertThat(inei.poblacion("150101")).isEqualTo(new Dato<>(Optional.of(POBLACION_LIMA), ALMACENADA));
		assertThat(inei.distritosEnRadio(-12.05, -77.03, 20)).isEqualTo(new Dato<>(enRadio, ALMACENADA));
	}

	@Test
	void guardaEnCacheLosDistritosDelRadio() {
		List<DistritoPoblado> enRadio = List.of(new DistritoPoblado(LIMA, POBLACION_LIMA));
		when(adapter.distritosEnRadio(-12.05, -77.03, 20)).thenReturn(enRadio);

		inei.distritosEnRadio(-12.05, -77.03, 20);
		Dato<List<DistritoPoblado>> segunda = inei.distritosEnRadio(-12.05, -77.03, 20);

		assertThat(segunda.valor()).isEqualTo(enRadio);
		assertThat(segunda.procedencia().origen()).isEqualTo(Origen.CACHE);
	}

	private static FuenteNoDisponibleException caido() {
		return new FuenteNoDisponibleException(Fuente.INEI, "Error al consultar el INEI", null);
	}

}
