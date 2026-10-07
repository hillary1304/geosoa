package com.geosoa.backend.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import com.geosoa.backend.adapter.igp.FiltroSismos;
import com.geosoa.backend.adapter.igp.IgpAdapter;
import com.geosoa.backend.domain.Dato;
import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Origen;
import com.geosoa.backend.domain.Sismo;
import com.geosoa.backend.persistence.SismoEntity;
import com.geosoa.backend.persistence.SismoRepository;
import com.geosoa.backend.shared.FuenteNoDisponibleException;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class IgpResilienteTests {

	private static final Sismo RECIENTE = sismo("2026-716", "2026-10-04T19:50:19-05:00", 3.5, "PASCO");

	private static final Sismo ANTERIOR = sismo("2026-700", "2026-10-01T08:00:00-05:00", 4.6, "AREQUIPA");

	@Autowired
	private SismoRepository repository;

	private IgpAdapter adapter;

	private IgpResiliente igp;

	@BeforeEach
	void configurar() {
		adapter = mock(IgpAdapter.class);
		Retry sinEspera = Retry.of("igp", RetryConfig.custom().maxAttempts(1).build());
		FuenteResiliente fuente = new FuenteResiliente(Fuente.IGP, Caffeine.newBuilder().build(),
				CircuitBreaker.ofDefaults("igp"), sinEspera);
		igp = new IgpResiliente(adapter, fuente, repository);
	}

	@Test
	void consultaElUltimoSismoEnVivo() {
		when(adapter.ultimoSismo()).thenReturn(Optional.of(RECIENTE));

		Dato<Optional<Sismo>> dato = igp.ultimoSismo();

		assertThat(dato.valor()).contains(RECIENTE);
		assertThat(dato.procedencia().origen()).isEqualTo(Origen.LIVE);
	}

	@Test
	void conElIgpCaidoDevuelveElUltimoSismoAlmacenado() {
		repository.save(SismoEntity.from(ANTERIOR));
		repository.saveAndFlush(SismoEntity.from(RECIENTE));
		when(adapter.ultimoSismo()).thenThrow(caido());

		Dato<Optional<Sismo>> dato = igp.ultimoSismo();

		assertThat(dato.valor()).contains(RECIENTE);
		assertThat(dato.procedencia().origen()).isEqualTo(Origen.DB);
		assertThat(dato.procedencia().actualizadoEn()).isNotNull();
	}

	@Test
	void conElIgpCaidoFiltraLosSismosAlmacenados() {
		repository.save(SismoEntity.from(ANTERIOR));
		repository.save(SismoEntity.from(RECIENTE));
		repository.saveAndFlush(SismoEntity.from(sismo("2026-650", "2026-09-20T10:00:00-05:00", 5.2, "AREQUIPA")));
		FiltroSismos filtro = new FiltroSismos(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 4), 4.0, "arequipa");
		when(adapter.sismosReportados(filtro, 0, 50)).thenThrow(caido());

		Dato<List<Sismo>> dato = igp.sismosReportados(filtro, 0, 50);

		assertThat(dato.valor()).containsExactly(ANTERIOR);
		assertThat(dato.procedencia().origen()).isEqualTo(Origen.DB);
	}

	@Test
	void ordenaYPaginaLosSismosAlmacenados() {
		repository.save(SismoEntity.from(ANTERIOR));
		repository.saveAndFlush(SismoEntity.from(RECIENTE));
		when(adapter.sismosReportados(FiltroSismos.sinFiltros(), 1, 1)).thenThrow(caido());

		assertThat(igp.sismosReportados(FiltroSismos.sinFiltros(), 1, 1).valor()).containsExactly(ANTERIOR);
	}

	@Test
	void sinSismosAlmacenadosInformaFuenteNoDisponible() {
		when(adapter.sismosReportados(FiltroSismos.sinFiltros(), 0, 50)).thenThrow(caido());

		assertThatThrownBy(() -> igp.sismosReportados(FiltroSismos.sinFiltros(), 0, 50))
			.isInstanceOf(FuenteNoDisponibleException.class);
	}

	@Test
	void distingueLasConsultasEnCache() {
		when(adapter.sismosReportados(FiltroSismos.sinFiltros(), 0, 50)).thenReturn(List.of(RECIENTE));
		when(adapter.sismosReportados(FiltroSismos.sinFiltros(), 1, 50)).thenReturn(List.of(ANTERIOR));

		igp.sismosReportados(FiltroSismos.sinFiltros(), 0, 50);
		Dato<List<Sismo>> segundaPagina = igp.sismosReportados(FiltroSismos.sinFiltros(), 1, 50);
		Dato<List<Sismo>> primeraOtraVez = igp.sismosReportados(FiltroSismos.sinFiltros(), 0, 50);

		assertThat(segundaPagina.valor()).containsExactly(ANTERIOR);
		assertThat(primeraOtraVez.valor()).containsExactly(RECIENTE);
		assertThat(primeraOtraVez.procedencia().origen()).isEqualTo(Origen.CACHE);
	}

	private static FuenteNoDisponibleException caido() {
		return new FuenteNoDisponibleException(Fuente.IGP, "Error al consultar el IGP", null);
	}

	private static Sismo sismo(String codigo, String fechaHora, double magnitud, String departamento) {
		return new Sismo(codigo, ZonedDateTime.parse(fechaHora).withZoneSameInstant(SismoEntity.ZONA_LIMA), -12.05,
				-77.03, 30, magnitud, null, "Referencia", departamento, false);
	}

}
