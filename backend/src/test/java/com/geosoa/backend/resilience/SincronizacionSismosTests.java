package com.geosoa.backend.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import com.geosoa.backend.adapter.igp.FiltroSismos;
import com.geosoa.backend.adapter.igp.IgpAdapter;
import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Sismo;
import com.geosoa.backend.persistence.SismoEntity;
import com.geosoa.backend.persistence.SismoRepository;
import com.geosoa.backend.shared.FuenteNoDisponibleException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SincronizacionSismosTests {

	@Autowired
	private SismoRepository repository;

	private IgpAdapter igp;

	private SincronizacionSismos sincronizacion;

	@BeforeEach
	void configurar() {
		igp = mock(IgpAdapter.class);
		sincronizacion = new SincronizacionSismos(igp, repository, 7);
	}

	@Test
	void guardaLosSismosNuevosYActualizaLosExistentes() {
		repository.saveAndFlush(SismoEntity.from(sismo("2026-716", 3.5)));
		when(igp.sismosReportados(any(), eq(0), eq(SincronizacionSismos.TAMANO_PAGINA)))
			.thenReturn(List.of(sismo("2026-716", 3.7), sismo("2026-717", 4.1)));

		sincronizacion.sincronizar();

		assertThat(repository.findAll()).hasSize(2);
		assertThat(repository.findByCodigo("2026-716").orElseThrow().toDomain().magnitud()).isEqualTo(3.7);
		assertThat(repository.findByCodigo("2026-717")).isPresent();
	}

	@Test
	void consultaLosUltimosDiasConfigurados() {
		when(igp.sismosReportados(any(), eq(0), eq(SincronizacionSismos.TAMANO_PAGINA))).thenReturn(List.of());

		sincronizacion.sincronizar();

		ArgumentCaptor<FiltroSismos> filtro = ArgumentCaptor.forClass(FiltroSismos.class);
		verify(igp).sismosReportados(filtro.capture(), eq(0), eq(SincronizacionSismos.TAMANO_PAGINA));
		assertThat(filtro.getValue().desde()).isEqualTo(LocalDate.now(SismoEntity.ZONA_LIMA).minusDays(7));
	}

	@Test
	void unaCaidaDelIgpNoInterrumpeLaTarea() {
		repository.saveAndFlush(SismoEntity.from(sismo("2026-716", 3.5)));
		when(igp.sismosReportados(any(), eq(0), eq(SincronizacionSismos.TAMANO_PAGINA)))
			.thenThrow(new FuenteNoDisponibleException(Fuente.IGP, "Error al consultar el IGP", null));

		sincronizacion.sincronizar();

		assertThat(repository.findAll()).hasSize(1);
	}

	private static Sismo sismo(String codigo, double magnitud) {
		return new Sismo(codigo, ZonedDateTime.parse("2026-10-04T19:50:19-05:00").withZoneSameInstant(SismoEntity.ZONA_LIMA),
				-10.56, -75.39, 10, magnitud, null, "2 km al NE de Oxapampa", "PASCO", true);
	}

}
