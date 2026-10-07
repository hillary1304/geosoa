package com.geosoa.backend.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.geosoa.backend.adapter.senamhi.CapaPrecipitacion;
import com.geosoa.backend.adapter.senamhi.SenamhiAdapter;
import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Origen;
import com.geosoa.backend.domain.Precipitacion;
import com.geosoa.backend.shared.FuenteNoDisponibleException;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

class SenamhiResilienteTests {

	private static final Precipitacion ANUAL = new Precipitacion("ANUAL", 10, 15.0);

	private SenamhiAdapter adapter;

	private SenamhiResiliente senamhi;

	@BeforeEach
	void configurar() {
		adapter = mock(SenamhiAdapter.class);
		FuenteResiliente fuente = new FuenteResiliente(Fuente.SENAMHI, Caffeine.newBuilder().build(),
				CircuitBreaker.ofDefaults("senamhi"), Retry.of("senamhi", RetryConfig.custom().maxAttempts(1).build()));
		senamhi = new SenamhiResiliente(adapter, fuente);
	}

	@Test
	void guardaEnCacheCadaPuntoYCapa() {
		when(adapter.precipitacion(-12.05, -77.03)).thenReturn(Optional.of(ANUAL));
		when(adapter.precipitacion(-12.05, -77.03, CapaPrecipitacion.ENERO))
			.thenReturn(Optional.of(new Precipitacion("ENERO", 0, 5.0)));

		assertThat(senamhi.precipitacion(-12.05, -77.03).procedencia().origen()).isEqualTo(Origen.LIVE);
		assertThat(senamhi.precipitacion(-12.05, -77.03, CapaPrecipitacion.ENERO).valor())
			.contains(new Precipitacion("ENERO", 0, 5.0));
		assertThat(senamhi.precipitacion(-12.05, -77.03).procedencia().origen()).isEqualTo(Origen.CACHE);
		verify(adapter, times(1)).precipitacion(-12.05, -77.03);
	}

	@Test
	void sinCacheNiRespaldoInformaFuenteNoDisponible() {
		when(adapter.precipitacion(-12.05, -77.03))
			.thenThrow(new FuenteNoDisponibleException(Fuente.SENAMHI, "Error al consultar el SENAMHI", null));

		assertThatThrownBy(() -> senamhi.precipitacion(-12.05, -77.03))
			.isInstanceOfSatisfying(FuenteNoDisponibleException.class,
					ex -> assertThat(ex.getFuente()).isEqualTo(Fuente.SENAMHI));
	}

}
